package io.github.baifangkual.jlib.db.impl.pool;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DB;
import io.github.baifangkual.jlib.db.DBC;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.PooledDBC;
import io.github.baifangkual.jlib.db.exception.CloseConnectionException;
import io.github.baifangkual.jlib.db.exception.DBConnectException;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.abs.BaseDBC;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * {@code PooledDBC} 实现，
 * <p>Connection 连接池的简单实现 内部有引用实际使用的数据源{@link DB}类型，
 * 该实现线程安全（应确保不同的线程持有不同的{@link Connection}对象），因该类型实现{@link PooledDBC}，所以对调用方来说，
 * 安全的使用该类型的方式应是通过 {@code PooledDBC} 接口或 {@code PooledDB} 接口
 * <p>
 * 该类型的实现的行为说明：<br>
 * 类型构造时需要参数maxPoolSize,表示连接池的最大大小<br>
 * 类型构造完成后，默认不会立即创建连接对象，当连接需求进来时{@link DBC#getConn()},将开始创建连接对象并管理其;<br>
 * 当连接需求进来时（外侧需求获取连接），该类型有空闲的连接对象时，将直接返回空闲的连接对象，若无空闲的连接对象且当前已创建的
 * 连接对象数小于{@link #maxPoolSize}值时，将会创建新的连接对象并返回，若无空闲的连接对象并且已创建的连接对象数量以达到
 * 最大值，调用方线程将在{@link DBC#getConn()}方法阻塞，直到有其他线程返回了连接对象（即其他线程使用完了连接对象，将二手连接对象返回了），
 * 才能够被唤醒并获取到连接对象；<br>
 * 当外侧调用该类型的{@link #close()}时，该首先会检查由该管理的所有连接对象是否已经被回收，若都已经被回收，则真正关闭所有连接对象，
 * 若有连接对象未被回收，则调用{@link #close()}的线程将阻塞一定时间在此，
 * 直到所有连接对象都已被其他线程使用完成（被回收）或超时之后才开始真正关闭连接池实例；<br>
 * 该类型实际返回的{@link Connection}实现为{@link OnCloseRecycleRefConnection},当该连接对象被关闭后，后续的在连接对象上的方法调用都将抛出异常;<br>
 * 当该类型被关闭时，后续的其他方法调用都将抛出异常，除了{@link #recycle(OnCloseRecycleRefConnection)}方法，因为该方法是回收连接对象的方法，
 * 但{@link #recycle(OnCloseRecycleRefConnection)}不应由该的使用方显式调用，因为使用方调用{@link Connection}的{@link Connection#close()}方法时，
 * 将会触发该回收方法，所以使用方仅需保证自己使用完链接对象后能够成功调用连接对象的{@link Connection#close()}方法即可;<br>
 * 该类型的{@link #close()}方法被调用后，将不能再次调用{@link #close()}方法，第二次调用该会抛出异常；<br>
 *
 * @author baifangkual
 * @implNote 该类型当前（20250609）实现的额外说明：<br>
 * 倘若外侧线程在返回借用时发生异常等情况，这可能会造成一定的问题，遂最好在try-with-resource语法块内使用获取的连接，即可保证调用使用Conn的close<br>
 * 当前实现未有类似于 借用检查器 的实现，遂没有针对连接被借用了而追查借用是否使用超时等机制<br>
 * 当前实现在回收连接对象时没有关闭 {@link Connection#createStatement()} 等机制，这些行为应由外侧调用方完成
 * <p>该实现目前没有向JMXBean注册监控，且不会启用额外的监控线程，所有的检查等，都是惰性的</p>
 * <p>该实现在接收到Conn对象借用请求时若当前无空闲Conn对象且Conn对象已达到最大值 {@link DBCCfgOptions#poolMaxSize}，
 * 则会等待 {@link DBCCfgOptions#poolMaxWaitBorrowInterval} 时间，
 * 若等待超时仍未获取到可用的Conn对象，则请求借用的线程会抛出异常，依此，若借用了Conn对象后一直没有归还（调用 Connection.close())，
 * 则可用的Conn会越来越少，直到可用的Conn数量耗尽，若这种情况发生，
 * 则后续所有的线程的Conn对象的借用请求都会等待 {@link DBCCfgOptions#poolMaxWaitBorrowInterval} 时间后抛出异常，连接池即完全不可用</p>
 * @see DB
 * @see io.github.baifangkual.jlib.db.PooledDB
 * @see PooledDBC
 * @see DBC
 * @see OnCloseRecycleRefConnection
 * @see OnCloseRecycleRefConnection#close()
 * @since 2024/7/25
 */
public class ConnPoolDBC extends BaseDBC
        implements Poolable<OnCloseRecycleRefConnection>, PooledDBC, Closeable {

    private static final Logger log = LoggerFactory.getLogger(ConnPoolDBC.class);
    private static final String CLOSED_MSG = "ConnPoolDBC closed";
    private static final AtomicInteger PoolIDGen = new AtomicInteger(0);

    private final int instanceId = PoolIDGen.getAndIncrement();
    private final AtomicBoolean open = new AtomicBoolean(true);
    // 因为queue中被借用后queueSize变小，遂需要该表示conn数量
    private final AtomicInteger currConnNum = new AtomicInteger(0);
    private final AtomicInteger connIdGenerator = new AtomicInteger(0);
    private final Lock lock = new ReentrantLock();
    // close await
    private final Condition cdClo = lock.newCondition();
    // borrow await
    private final Condition cdBor = lock.newCondition();
    // max
    private final int maxPoolSize;
    // ref real ,read dbc config in absDBC readonlyCfg
    // this class can use readonlyCfg() get that.
    private final DB realDB;
    // provider real, if not DBC, this is null
    // 因为DB不会提供MetaProvider，当对ConnPoolDBC使用PooledDB类型引用时，
    // 将不会对外暴露tablesMeta，columnsMeta等由MetaProvider提供的方法
    private final MetaProvider nullableMetaProvider;
    // 检查间隔
    private final long checkConnAliveIntervalMillis;
    // 等待借用最大时间
    private final long poolMaxWaitBorrowIntervalNanos;
    // 等待关闭最大时间
    private final long poolOnCloseWaitAllConnRecycleIntervalNanos;

    private final BlockingDeque<OnCloseRecycleRefConnection> queue;
    private final BlockingDeque<OnCloseRecycleRefConnection> inUsedQueue;

    public ConnPoolDBC(BaseDBC dbc, int maxPoolSize) {
        this(dbc, dbc.readonlyCfg(), dbc.metaProvider(), maxPoolSize);
    }

    public ConnPoolDBC(DB db, int maxPoolSize) {
        this(db, Cfg.readonlyEmpty(), null, maxPoolSize);
    }

    private ConnPoolDBC(DB db, Cfg cfg, MetaProvider nullableMetaProvider, int maxPoolSize) {
        super(cfg);
        Err.realIf(maxPoolSize < 1, IllegalDBCCfgException::new, "maxPoolSize < 1");
        this.realDB = db;
        this.nullableMetaProvider = nullableMetaProvider;
        this.maxPoolSize = maxPoolSize;
        this.queue = new LinkedBlockingDeque<>(maxPoolSize);
        this.inUsedQueue = new LinkedBlockingDeque<>(maxPoolSize);
        Duration checkDur = cfg.getOrDefault(DBCCfgOptions.poolCheckConnAliveInterval);
        if (checkDur.isNegative()) {
            throw new IllegalArgumentException("poolCheckConnAliveInterval is negative");
        }
        this.checkConnAliveIntervalMillis = checkDur.toMillis();
        Duration waitBorrowDur = cfg.getOrDefault(DBCCfgOptions.poolMaxWaitBorrowInterval);
        if (waitBorrowDur.isNegative()) {
            throw new IllegalArgumentException("poolMaxWaitBorrowInterval is negative");
        }
        this.poolMaxWaitBorrowIntervalNanos = waitBorrowDur.toNanos();
        Duration onCloseWaitDur = cfg.getOrDefault(DBCCfgOptions.poolOnCloseWaitAllConnRecycleInterval);
        if (onCloseWaitDur.isNegative()) {
            throw new IllegalArgumentException("poolOnCloseWaitAllConnRecycleInterval is negative");
        }
        this.poolOnCloseWaitAllConnRecycleIntervalNanos = onCloseWaitDur.toNanos();
        if (log.isDebugEnabled()) {
            log.debug("create PooledDBC, maxPoolSize: {}, realDBC:{}", this.maxPoolSize, this.realDB);
        }
    }

    int instanceId() {
        return this.instanceId;
    }

    @Override
    public String jdbcUrl() {
        return realDB.jdbcUrl();
    }

    @Override
    public PooledDBC pooled(int maxPoolSize) {
        return new ConnPoolDBC(this.realDB, readonlyCfg(), this.nullableMetaProvider, maxPoolSize);
    }

    private Connection newConn() throws Exception {
        return this.realDB.getConn();
    }


    private OnCloseRecycleRefConnection newWrapConn() throws Exception {
        if (currConnNum.get() >= maxPoolSize) {
            throw new IllegalStateException(Stf
                    .f("连接池持有的连接已到达最大连接数，不可再创建新连接, max:{}", maxPoolSize));
        }
        currConnNum.incrementAndGet();
        final int curId = connIdGenerator.incrementAndGet();
        Connection connRef = newConn();
        return new OnCloseRecycleRefConnection(curId, connRef, this, connRef.getAutoCommit());
    }

    /**
     * 校验Conn内实际的Conn是否可使用，因为数据库端可能会关闭闲置已久的连接对象
     */
    private boolean isConnAlive(OnCloseRecycleRefConnection conn) {
        Connection realConn = conn.realConnection();
        try {
            if (realConn.isClosed()) return false;
            // 3秒超时校验
            if (!realConn.isValid(3)) return false;
            this.realDB.fnAssertValidConnect().assertIsValid(realConn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 外界调用，借用一个Conn，当有空闲的conn对象，直接返回conn，若无空闲，则尝试创建一个新的conn对象并返回，
     * 当连接对象conn数量已经到达{@link #maxPoolSize}则不会再创建，调用线程将阻塞，直到有至少一个空闲的conn对象出现
     *
     * @return proxy {@link OnCloseRecycleRefConnection} impl {@link Connection}
     */
    @Override
    public OnCloseRecycleRefConnection borrow() {
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("borrowing, call thread: {}", curt.getName());
        }
        if (!open.get()) {
            throw new IllegalStateException(CLOSED_MSG);
        }
        lock.lock();
        try {
            OnCloseRecycleRefConnection ref = null;
            /*
            dcl检查
             */
            if (!open.get()) {
                throw new IllegalStateException(CLOSED_MSG);
            }
            // 系统调用获取一次当前时间，在while内使用之
            long curTime = System.currentTimeMillis();
            // 当队列中有可用的，直接拿一个
            while (!queue.isEmpty()) {
                // 可能因为闲置太久，已经被数据库端关闭，需关闭并丢弃之
                // 队列中无，表示大伙都忙，忙，忙点好阿，
                // 否则，判断是否可再创建，若可再创建，创建之，直接使用，因回收时会将其添加到队列，所以不用管
                OnCloseRecycleRefConnection one = queue.take();
                // 一定不为空，因为在队列里的都是已经更新最后使用时间的了
                long lastUsed = one.lastUsedTimeMillis();
                // 判定是否需要检查
                if (!(curTime - lastUsed > checkConnAliveIntervalMillis)) {
                    ref = one; // 不需要检查直接赋值并跳出while
                    break;
                } else {
                    // 若可用，则赋值引用，跳出while
                    // 因为是queue，所以后一个的 lastUsed 时间一定大于当前的，当前活着，则后面都活着
                    // 若ref内不存储 lastUsed 信息，则次大于检查间隔时都要进行至少一个 isConnAlive 检查...
                    // 遂ref内存储 lastUsed 信息，即每次归还都进行系统调用记录最后使用时间
                    if (isConnAlive(one)) {
                        ref = one;
                        break;
                    } else {
                        // 已被数据库断开连接的，回收资源并尝试下一个
                        realCloseConn(one, true);
                        // 丢弃了一个，更新当前池状态大小 -1
                        currConnNum.decrementAndGet();
                    }
                }
            }
            // while完了，队列空了还没有任何一个可用的，那就创建新的
            if (ref == null) {
                if (currConnNum.get() < maxPoolSize) {
                    ref = newWrapConn();
                    // 否则，表示已经不能再创建了，线程阻塞至此
                } else {
                    // 终止条件：1.已经被标记为关闭，由close唤醒
                    // 2.由 recycle唤醒，可以拿
                    long remainingNanos = this.poolMaxWaitBorrowIntervalNanos;
                    while (open.get() && queue.isEmpty()) {
                        if (remainingNanos <= 0L) {
                            // 超时仍未拿到可用的，抛出异常
                            throw new DBConnectException(
                                    Stf.f("thread '{}' wait for borrow Connection timeout, wait seconds: {}," +
                                          " pool current Connection: {}, inUsed: {}",
                                            Thread.currentThread().getName(),
                                            Duration.ofNanos(this.poolMaxWaitBorrowIntervalNanos).toSeconds(),
                                            currConnNum.get(),
                                            inUsedQueue.size())
                            );
                        }
                        remainingNanos = cdBor.awaitNanos(remainingNanos);
                    }
                    // 判断是否已被标记为关闭，若已被标记未关闭，则当前线程抛出异常
                    // 若不是，则直接从 recycle 回收的队列中拿一个
                    if (open.get()) {
                        // 这里不用判断是否过期，因为是被唤醒的，遂一定拿到刚被用完的
                        // 但需注意，外界借用后长期不使用，可能导致已被服务器端关闭
                        ref = queue.take();
                    } else throw new IllegalStateException(CLOSED_MSG);
                }
            }
            ref.borrowBef(); //将使用标志置位未使用 cas操作，当外界非法使用该时，会抛出异常
            // 标记为正使用的
            inUsedQueue.add(ref);
            if (log.isDebugEnabled()) {
                final Thread curt = Thread.currentThread();
                log.debug("borrowed, proxyConn:{}, call thread: {}", ref, curt.getName());
            }
            return ref;
        } catch (InterruptedException e) {
            log.error("线程收到中断，恢复中断信号，由外层处理");
            Thread t = Thread.currentThread();
            t.interrupt();
            throw new IllegalStateException("thread: " + t.getName() + " interrupt.", e);
        } catch (Exception e) {
            // 该处的异常是由创建conn时可能产生的，将直接向外侧抛出
            if (e instanceof RuntimeException rtE) throw rtE;
            else throw new DBConnectException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 回收一个{@link OnCloseRecycleRefConnection}对象，使用完的conn将通过该接口回收，
     * 调用方无需使用该方法，该方法由{@link OnCloseRecycleRefConnection#close()}触发
     *
     * @param conn proxy {@link OnCloseRecycleRefConnection} 连接对象
     */
    @Override
    public void recycle(OnCloseRecycleRefConnection conn) {
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("recycling, proxyConn:{}, call thread: {}", conn, curt.getName());
        }
        /*
        因conn的线程不安全，使用方两个线程不会持有同一个引用，这里无需锁
        也因为当一个线程调用close时，会等待被借用的conn回到pool中才能够执行关闭
        那个线程会持有锁，遂该处若线程先持有锁情况下，close线程会阻塞，这是正常情况，
        而当close线程先持有锁并阻塞等待conn都被回收的情况下，该处线程因获取不到锁而导致造成死锁

        20240729更新逻辑：这里使用锁，close不自旋，并且不应相信调用方，应判断是否已经在池中,
        并且该头部不应判断标志位？（是否要相信调用方）（结论：recycle应该宽一点）
         */
        if (!conn.isClosed()) {
            // 若能进入该if，则证明未按API说明使用，OnCloseRecycleRefConn在recycleSelf中已置位un_used
            log.warn("回收的 {} 对象仍处于inUsed状态，" +
                     "调用方不应显式调用该方法，该方法由OnCloseRecycleRefConnection.close()调用", conn);
            lock.lock();
            try { //dcl，调用方可能持有同一个
                if (!conn.isClosed()) {
                    conn.close();
                }
            } finally {
                lock.unlock();
            }
        }
        lock.lock();
        try {
            /*
            20240729：
            因为 OnCloseRecycleRefConn 的 id 始终为从atomic中生产
            且未标记自己所属哪个池，所以一个池生产的conn回收到另一个池时，会破坏连接和连接池对象的安全性
            所幸调用方不会摸到较为里面的方法，该问题产生的概率不大，若按照API文档正确使用此类型，则不会产生这种问题
            已有修复方案，但尚未有时间修复，可用 poolId << 32 + connId 作为 connId即可
             */
            if (!queue.contains(conn)) {
                // 在加到队列和唤醒等待线程前清理状态
                cleanRealConnStateSneaky(conn);
                if (!inUsedQueue.remove(conn)) { // 该情况不会发生，仅为屏蔽idea烦人提示
                    throw new IllegalStateException("inUsed queue not found connection: " + conn);
                }
                queue.add(conn);
                cdBor.signalAll();
                cdClo.signalAll();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("{} 已经被回收，无视该次回收操作", conn);
                }
            }
        } finally {
            lock.unlock();
        }
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("recycled, proxyConn:{}, call thread: {}", conn, curt.getName());
        }
    }

    void cleanRealConnStateSneaky(OnCloseRecycleRefConnection refConn) {
        // 参考Hikari，如果不是 autoCommit 状态，清理事务回滚、清理warn
        Connection realConn = refConn.realConnection();
        try {
            boolean autoCommit = refConn.proxyIsAutoCommit();
            if (!autoCommit) {
                realConn.rollback();
            }
            realConn.clearWarnings();
        } catch (SQLException ignore) { /* ignore */ }
    }

    /**
     * 真正关闭实际的conn对象，原本该方法为无参的closeAllConn，但这样会使前面关闭的conn的异常被抛出从而打断
     * 后面要关闭的conn的关闭，遂应当每个都调用close并在异常时addSuppressed,{@link #realCloseAll()}
     *
     * @param refConn conn代理
     */
    private void realCloseConn(OnCloseRecycleRefConnection refConn, boolean ignoreOnCloseErr) throws Exception {
        if (ignoreOnCloseErr) {
            try {
                refConn.realConnection().close();
            } catch (Exception e) { /* ignore */ }
        } else {
            refConn.realConnection().close();
        }
    }

    /**
     * 尝试关闭每个持有的conn，当任意一个close出现异常，该方法会在都尝试关闭后才抛出异常，
     * 该方法不会简单的吞掉close (use {@code addSuppressed})
     *
     * @throws CloseConnectionException 当有任意一个close异常时，先关闭能关闭的，然后抛出异常
     */
    private void realCloseAll() throws CloseConnectionException {
        lock.lock();
        try {
            boolean panic = false;
            StringJoiner msg = new StringJoiner(",\n");
            List<Exception> onCloseErrs = new ArrayList<>(queue.size() + inUsedQueue.size());
            // 包括在使用的和未被使用的，都关闭其
            List<OnCloseRecycleRefConnection> allRef = Stream.concat(queue.stream(), inUsedQueue.stream()).toList();
            for (OnCloseRecycleRefConnection refC : allRef) {
                try {
                    boolean inUsed = !refC.isClosed();
                    realCloseConn(refC, false);
                    refC.close();
                    if (inUsed) {
                        log.warn("Force closed in-use Connection: {}", refC);
                    }
                } catch (Exception e) {
                    log.error("err on closing Connection: {}", refC);
                    // 异常原因最后抛出，不打断 for 内逐个关闭 conn的过程
                    onCloseErrs.add(e);
                    // do flag throw true
                    panic = true;
                    /*
                    1.error of...,
                    2.error of...,
                     */
                    msg.add(refC.connId() + ". [" + refC + "]: " + e.getMessage());
                }
            }
            if (panic) {
                CloseConnectionException eThrow = new CloseConnectionException(msg.toString());
                onCloseErrs.forEach(eThrow::addSuppressed);
                throw eThrow;
            }
        } finally {
            lock.unlock();
        }
    }

    /*
    to do Cleaner test impl
        private static final Cleaner CLEANER = Cleaner.create();
        private static final class PooledBDCClean {}
     */

    @Override
    public void close() throws Exception {
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("closing pool proxy connection, call thread: {}", curt.getName());
        }
        /*
        因锁的关系，当close调用时应立即将标志位OPEN置位false，否则borrow还会可能发生
         */
        if (!open.get()) {
            throw new IllegalStateException(CLOSED_MSG);
        }
        lock.lock();
        try {
            // 立即将标志位置位false，即当前被调用并持有锁时，不允许再borrow
            if (open.compareAndSet(true, false)) {
                // 唤醒所有等待拿的线程，因为这里已经被标记为关闭，所以所有要借用的线程抛出异常
                cdBor.signalAll();
                long onCloseWaitNanos = this.poolOnCloseWaitAllConnRecycleIntervalNanos;
                while (queue.size() < currConnNum.get()) {
                    // sleep ...等待所有借用被归还，防止伪唤醒
                    // 这里若唤醒后queue中仍没有全部被归还，则继续等待
                    // 即该方法内，若超时，则强制关闭所有连接，即使外界仍在使用
                    // 若等待期间线程收到中断信号，则不会进行realCloseAll的清理了
                    if (onCloseWaitNanos <= 0) {
                        // 如果超时，则跳出循环并清理
                        log.warn("thread {} call pool.close() wait all Connection recycle timeout," +
                                 " wait seconds: {}," +
                                 " pool current Connection: {}," +
                                 " inUsed: {}, available: {}," +
                                 " now Force closing in-use Connections",
                                Thread.currentThread().getName(),
                                Duration.ofNanos(this.poolOnCloseWaitAllConnRecycleIntervalNanos).toSeconds(),
                                currConnNum.get(),
                                inUsedQueue.size(),
                                queue.size());
                        break;
                    }
                    onCloseWaitNanos = cdClo.awaitNanos(onCloseWaitNanos);
                }
                realCloseAll();
                // 清理队列，丢弃引用
                this.inUsedQueue.clear();
                this.queue.clear();
            } else throw new IllegalStateException(CLOSED_MSG);
        } finally {
            lock.unlock();
        }
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("closed pool, call thread: {}", curt.getName());
        }
    }

    @Override
    public boolean isClosed() {
        return !open.get();
    }

    @Override
    public MetaProvider metaProvider() {
        return nullableMetaProvider;
    }

    @Override
    protected void throwOnIllegalCfg(Cfg cfg) throws IllegalDBCCfgException {
        /* do nothing...*/
    }

    @Override
    public ConnPoolDBC assertConnect() throws DBConnectException {
        Err.realIf(!open.get(), DBConnectException::new, CLOSED_MSG);
        realDB.assertConnect();
        return this;
    }

    @Override
    public FnAssertValidConnect fnAssertValidConnect() {
        return realDB.fnAssertValidConnect();
    }

    @Override
    public Connection getConn() throws SQLException {
        // 这里不用判断 open，因为borrow内有该判断
        return borrow();
    }

}
