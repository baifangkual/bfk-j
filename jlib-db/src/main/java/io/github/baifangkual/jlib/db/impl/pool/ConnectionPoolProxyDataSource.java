package io.github.baifangkual.jlib.db.impl.pool;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.*;
import io.github.baifangkual.jlib.db.exception.ConnectionCloseFailException;
import io.github.baifangkual.jlib.db.exception.DataSourceConnectionFailException;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author baifangkual
 * create time 2024/7/25
 * <p>
 * CloseableDataSource实现，
 * Connection 连接池的简单实现 内部有引用实际使用的数据源{@link DBC}类型，
 * 该实现线程安全（应确保不同的线程持有不同的{@link Connection}对象），因该类型实现{@link DBCPool}，所以对调用方来说，
 * 安全的使用该类型的方式应是通过CloseableDataSource接口<br>
 * <p>
 * 该类型的实现的行为说明：<br>
 * 类型构造时需要参数maxPoolSize,表示连接池的最大大小;<br>
 * 类型构造完成后，默认不会立即创建连接对象，当连接需求进来时{@link #getConn()},将开始创建连接对象并管理其;<br>
 * 当连接需求进来时（外侧需求获取连接），该类型有空闲的连接对象时，将直接返回空闲的连接对象，若无空闲的连接对象且当前已创建的
 * 连接对象数小于{@link #maxPoolSize}值时，将会创建新的连接对象并返回，若无空闲的连接对象并且已创建的连接对象数量以达到
 * 最大值，调用方线程将在{@link #getConn()}方法阻塞，直到有其他线程返回了连接对象（即其他线程使用完了连接对象，将二手连接对象返回了），
 * 才能够被唤醒并获取到连接对象；<br>
 * 当外侧调用该类型的{@link #close()}时，该首先会检查由该管理的所有连接对象是否已经被回收，若都已经被回收，则真正关闭所有连接对象，
 * 若有连接对象未被回收，则调用{@link #close()}的线程将阻塞在此，直到所有连接对象都已被其他线程使用完成（被回收）才开始真正关闭该类型；<br>
 * 该类型实际返回的{@link Connection}实现为{@link OnCloseRecycleRefConnection},当该连接对象被关闭时，后续的在连接对象上的方法调用都将抛出异常;<br>
 * 当该类型被关闭时，后续的其他方法调用都将抛出异常，除了{@link #recycle(OnCloseRecycleRefConnection)}方法，因为该方法是回收连接对象的方法，
 * 但{@link #recycle(OnCloseRecycleRefConnection)}不应由该的使用方显式调用，因为使用方调用{@link Connection}的{@link Connection#close()}方法时，
 * 将会触发该回收方法，所以使用方仅需保证自己使用完链接对象后能够成功调用连接对象的{@link Connection#close()}方法即可;<br>
 * 该类型的{@link #close()}方法被调用后，将不能再次调用{@link #close()}方法，第二次调用该会抛出异常；<br>
 * 该类型当前（20240730）实现的额外说明：<br>
 * 实现后进行了简单测试检查其是否存在线程安全问题，未见线程安全问题，未见死锁<br>
 * 当前实现未有类似于 await(time) 方法，遂因无空闲连接及close时有对象未被回收的情况，调用的线程会被无限期阻塞，
 * 倘若外侧线程在返回借用时发生异常等情况，这可能会造成一定的问题<br>
 * 当前实现未有类似于 借用检查器 的实现，遂没有针对连接被借用了而追查借用是否使用超时等机制<br>
 * 当前实现的借用回收{@link #recycle(OnCloseRecycleRefConnection)} 未有检查被借用对象是否属于自己的检查<br>
 * 当前实现在回收连接对象时没有回滚、提交、关闭{@link Connection}对象的{@link Connection#createStatement()}等机制，这些行为应由外侧调用方完成<br>
 * 当前实现未有连接保活等措施，遂当前连接对象活性及生命周期等，依赖于数据库自身的实现（conn是否需要保活我未知）<br>
 * 有计划后续实现类似 reOpen 方法，但我要开始写文档了，先就不写该实现了<br>
 * 该类型的实现的各方面（性能、安全性等）需持续优化或改进，后续或可应实现类似 reOpen方法 <br>
 * <p>
 * 调用方不应该摸到这里，该的构造方式应当为{@link DBCFactory#createConnPool(Cfg)}等,
 * 使用方式见{@link DBCPool}说明<br>
 * 或者，你已有{@link DBC}类型实例，则可使用{@link #ConnectionPoolProxyDataSource(DBC, int)} 创建该<br>
 * @see DBCPool
 * @see DBCPool#getConn()
 * @see DBC
 * @see OnCloseRecycleRefConnection
 * @see OnCloseRecycleRefConnection#close()
 */
@Slf4j
public class ConnectionPoolProxyDataSource implements Pool<OnCloseRecycleRefConnection>, DBCPool {

    private static final String CLOSED_MSG = "该CloseableDataSource已经被关闭";

    private final AtomicBoolean open = new AtomicBoolean(true);
    private final Lock lock = new ReentrantLock();
    // close await
    private final Condition cdClo = lock.newCondition();
    // borrow await
    private final Condition cdBor = lock.newCondition();

    private final int maxPoolSize;
    private final AtomicInteger curr = new AtomicInteger(0);
    // 该当前未用，因为就一个maxPoolSize参数，后续可能多，即有用
    private final Cfg poolConfig;
    private final Cfg connConfig;
    private final DBC dataSource;
    private final MetaProvider metaProvider;

    private final BlockingDeque<OnCloseRecycleRefConnection> queue;


    public ConnectionPoolProxyDataSource(Cfg config) {
        this(DBCFactory.create(config));
    }

    public ConnectionPoolProxyDataSource(DBC dataSource) {
        this(dataSource, dataSource.cfg()
                .getOrDefault(DBCCfgOptions.CONN_POOL_CONFIG)
                .getOrDefault(DBCCfgOptions.CONN_POOL_MAX_SIZE));
    }

    public ConnectionPoolProxyDataSource(DBC dataSource, int maxPoolSize) {
        this.dataSource = dataSource;
        this.metaProvider = dataSource.metaProvider();
        this.connConfig = dataSource.cfg();
        this.poolConfig = connConfig.getOrDefault(DBCCfgOptions.CONN_POOL_CONFIG);
        this.maxPoolSize = maxPoolSize;
        this.queue = new LinkedBlockingDeque<>(maxPoolSize);
        if (log.isDebugEnabled()) {
            log.debug("create pool proxy connection, maxPoolSize: {}, dataSource:{}", this.maxPoolSize, this.dataSource);
        }
    }


    private Connection createNewConnection() throws Exception {
        return this.dataSource.getConn();
    }

    private OnCloseRecycleRefConnection createNewWrapConn() throws Exception {
        if (curr.get() >= maxPoolSize) {
            throw new IllegalStateException(Stf
                    .f("连接池持有的连接已到达最大连接数，不可再创建新连接, max:{}", maxPoolSize));
        }
        final int curId = curr.incrementAndGet();
        /*
        当高并发情况下，多个线程涌进来时，可能第一层条件尚未拦截到，遂该处还应该拦截一次
        20240729更改：borrow使用锁，不会有多个进来，但该处先不删
         */
        if (curId > maxPoolSize) {
            curr.set(maxPoolSize);
            throw new IllegalStateException(Stf
                    .f("连接池持有的连接已到达最大连接数，不可再创建新连接, max:{}", maxPoolSize));
        }
        Connection connRef = createNewConnection();
        return new OnCloseRecycleRefConnection(curId, connRef, this);
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
            OnCloseRecycleRefConnection ref;
            /*
            dcl检查
             */
            if (!open.get()) {
                throw new IllegalStateException(CLOSED_MSG);
            }
            // 当队列中有可用的，直接拿一个
            if (!queue.isEmpty()) {
                ref = queue.take();
                // 队列中无，表示大伙都忙，忙，忙点好阿，
                // 否则，判断是否可再创建，若可再创建，创建之，直接使用，因回收时会将其添加到队列，所以不用管
            } else if (curr.get() < maxPoolSize) {
                ref = createNewWrapConn();
                // 否则，表示已经不能再创建了，线程阻塞至此
            } else {
                // 终止条件：1.已经被标记为关闭，由close唤醒
                // 2.由 recycle唤醒，可以拿
                while (open.get() && queue.isEmpty()) {
                    cdBor.await();
                }
                // 判断是否已被标记为关闭，若已被标记未关闭，则当前线程抛出异常
                // 若不是，则直接从 recycle 回收的队列中拿一个
                if (open.get()) {
                    ref = queue.take();
                } else throw new IllegalStateException(CLOSED_MSG);
            }
            ref.borrowBef(); //将使用标志置位未使用 cas操作，当外界非法使用该时，会抛出异常
            if (log.isDebugEnabled()) {
                final Thread curt = Thread.currentThread();
                log.debug("borrowed, proxyConn:{}, call thread: {}", ref, curt.getName());
            }
            return ref;
        } catch (InterruptedException e) {
            log.error("线程收到中断，恢复中断信号，由外层处理");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // 该处的异常是由创建conn时可能产生的，将直接向外侧抛出
            // Err.throwPanic(e);
            throw new DataSourceConnectionFailException(e);
        } finally {
            lock.unlock();
        }
        // 不会运行至此，只为编译通过
        throw new IllegalStateException();
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

    /**
     * 真正关闭实际的conn对象，原本该方法为无参的closeAllConn，但这样会使前面关闭的conn的异常被抛出从而打断
     * 后面要关闭的conn的关闭，遂应当每个都调用close并吞掉异常,{@link #realCloseAll()}
     *
     * @param refConn conn代理
     */
    private void realCloseConn(OnCloseRecycleRefConnection refConn) throws Exception {
        refConn.realConnection().close();
    }

    /**
     * 尝试关闭每个持有的conn，当任意一个close出现异常，该方法会在都尝试关闭后才抛出异常，
     * 该方法不会简单的吞掉close
     *
     * @throws Exception 当有任意一个close异常时，先关闭能关闭的，然后抛出异常
     */
    private void realCloseAll() throws Exception {
        int i = 0;
        boolean panic = false;
        StringJoiner msg = new StringJoiner(",\n");
        lock.lock();
        try {
            for (OnCloseRecycleRefConnection refC : queue) {
                try {
                    realCloseConn(refC);
                } catch (Exception e) {
                    log.error("关闭 {} 对象过程中发生异常", refC.realConnection());
                    // do flag throw true
                    panic = true;
                    /*
                    1.error of...,
                    2.error of...,
                     */
                    msg.add(++i + ". [" + refC + "]: " + e.getMessage());
                }
            }
            if (panic) {
                throw new ConnectionCloseFailException(msg.toString());
            }
        } finally {
            lock.unlock();
        }
    }


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
                while (queue.size() < curr.get()) {
                    // sleep ...等待所有借用被归还，防止伪唤醒
                    cdClo.await();
                }
                realCloseAll();
            } else throw new IllegalStateException(CLOSED_MSG);
        } finally {
            lock.unlock();
        }
        if (log.isDebugEnabled()) {
            final Thread curt = Thread.currentThread();
            log.debug("closed pool proxy connection, call thread: {}", curt.getName());
        }
    }

    @Override
    public boolean isClosed() {
        return !open.get();
    }

    @Override
    public Cfg cfg() {
        Err.realIf(!open.get(), IllegalStateException::new, CLOSED_MSG);
        return connConfig;
    }

    @Override
    public MetaProvider metaProvider() {
        Err.realIf(!open.get(), IllegalStateException::new, CLOSED_MSG);
        return metaProvider;
    }

    @Override
    public void checkConn() throws Exception {
        Err.realIf(!open.get(), IllegalStateException::new, CLOSED_MSG);
        dataSource.checkConn();
    }

    @Override
    public Connection getConn() throws Exception {
        return borrow();
    }

    @Override
    public String getJdbcUrl() {
        return dataSource.getJdbcUrl();
    }

    @Override
    public List<Table.Meta> tablesMeta() {
        Err.realIf(!open.get(), IllegalStateException::new, CLOSED_MSG);
        return DBCPool.super.tablesMeta();
    }

}
