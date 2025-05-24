package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.mark.Default;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>ID Generator</b><br>
 * ID生成器，线程安全，生成的ID为雪花ID，共 63 位有效位，符号位永远为 0，
 * 最多可生成 {@code 2^62 - 1} 个ID<br>
 * 63 个有效位中 {@value TIMESTAMP_BITS} 位为时间偏移量，{@value MACHINE_ID_BITS} 位为 machineId 标识，
 * {@value SEQUENCE_BITS} 位为同毫秒细分的序列号<br>
 * 该Id构成: {@code 0(符号位) - 时间偏移量 - 机器码 - 同毫秒序列号}<br>
 * 该算法及代码参考：
 * <a href="https://zh.wikipedia.org/wiki/%E9%9B%AA%E8%8A%B1%E7%AE%97%E6%B3%95">雪花算法</a>
 * <a href="https://gitee.com/yu120/sequence/blob/master/src/main/java/cn/ms/sequence/Sequence.java">分布式高效ID生产黑科技</a>
 * <a href="https://github.com/beyondfengyu/SnowFlake">beyondfengyu/SnowFlake</a>
 * <a href="https://github.com/twitter-archive/snowflake/tree/b3f6a3c6ca8e1b6847baa6ff42bf72201e2c2231">twitter-archive/snowflake</a>
 *
 * @author baifangkual
 * @since 2024/12/12 v0.0.3
 */
@Default.prov(method = "defaultSingle")
public final class Idg {

    private static class SingleHolder {
        private static final Idg INSTANCE = new Idg();
    }

    private static Idg defaultSingle() {
        return SingleHolder.INSTANCE;
    }

    /**
     * 返回一个雪花ID
     *
     * @return 雪花ID
     * @throws IllegalStateException 当时间发生至少两次连续回溯,
     *                               或时间发生一次大于 {@value MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY} ms 的回溯时
     * @throws IllegalStateException 当线程收到中断信号时
     * @see Idg#nextId()
     */
    public static long longId() {
        return defaultSingle().nextId();
    }

    // DEFINITION ==================================
    /**
     * 同毫秒区分-机器码区分，占多少bit，机器最多区分 {@value MAX_MACHINE_ID_VALUE} 个
     */
    static final long MACHINE_ID_BITS = 10L;
    /**
     * 同毫秒区分-序列码区分，占多少bit，每毫秒内最多可生成的ID数: {@value MAX_SEQUENCE_VALUE} 个
     */
    static final long SEQUENCE_BITS = 11L;
    /**
     * 时间戳占用位数
     */
    static final long TIMESTAMP_BITS = 63L - MACHINE_ID_BITS - SEQUENCE_BITS;
    /**
     * 宇宙大爆炸时间（不是<br>
     * 当前设定值 1609459200000L 表示时间 2021-01-01 00:00:00(UTC/GMT+08:00)<br>
     * 不能在 {@code System.currentTimeMillis()} 小于该值时使用
     */
    static final long EPOCH_BEGIN = 1609459200000L;
    /**
     * 宇宙热寂时间（不是<br>
     * {@value EPOCH_BEGIN} + {@code (~(-1L << TIMESTAMP_BITS))} = 2160-05-15 15:35:11(UTC/GMT+08:00)<br>
     * 不能在 {@code System.currentTimeMillis()} 大于该值时使用
     */
    static final long EPOCH_END = EPOCH_BEGIN + ((1L << TIMESTAMP_BITS) - 1);
    /**
     * 最大的容忍回溯和闰秒的毫秒数，该值不宜过大，因为对回溯的抵抗将由线程的TIME-SLEEP负责
     */
    static final long MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY = 5L;

    // 最大值
    static final long MAX_MACHINE_ID_VALUE = ~(-1L << MACHINE_ID_BITS);
    static final long MAX_SEQUENCE_VALUE = ~(-1L << SEQUENCE_BITS);

    // 时间戳 偏移量
    static final long TIMESTAMP_OFFSET = MACHINE_ID_BITS + SEQUENCE_BITS;

    // 区分-机器码
    private final long machineId;

    // 获取当前时间(UTC)与 1970-01-01 00:00:00(UTC)之间的差值（以毫秒为单位）的方法引用
    private final FnGenCurrTimeMillis fnGenCurrTimeMillis;
    private final Lock lock = new ReentrantLock();
    // MUTABLE ===================================== （sync方法内，该不用原子）
    // 同毫秒内生成的id的序列号
    private long sequence = 0;
    // 上次生产 ID 时间戳, 初始值设定为 -1L
    private long lastTime = -1L;

    /**
     * 默认使用 mac 地址后 16位 和 当前计算机名+进程pid哈希后 {@value #MACHINE_ID_BITS} 位及 {@link System#currentTimeMillis()} 方法引用构造实例
     */
    public Idg() {
        this(Objects.hash(macLast16bit(), machineId10Bit()) & MAX_MACHINE_ID_VALUE,
                /*
                该值的粒度取决于底层作系统。例如，许多作系统以数十毫秒为单位测量时间。
                 */
                System::currentTimeMillis);
    }

    /**
     * 函数，该函数每次调用应返回当前时间(UTC)与 1970-01-01 00:00:00(UTC)之间的差值（以毫秒为单位）<br>
     * 可以引用 java的 {@link System#currentTimeMillis()} 方法，但该方法返回的值的粒度取决于底层作系统<br>
     * 该 {@link #Idg()} 的无参构造会将该函数设定为 {@link System#currentTimeMillis()}<br>
     *
     * @apiNote 若 {@link #Idg(long, FnGenCurrTimeMillis)} 构造中设定该函数返回常量，
     * 则 {@link Idg} 会在 {@link #sequence} 达到最大值 {@value MAX_SEQUENCE_VALUE}
     * 且接收到生成ID的请求时在方法 {@link #loop2NextTime(long)} 中死循环，遂请确保该函数的正确性，
     * 同理，该函数返回的值精度越高，则 {@link Idg} 实例性能越好<br>
     * 该函数每次调用返回的值最好大于等于上一次调用返回的值，在最坏的情况下（闰秒或时间回溯），
     * 允许该函数返回与上一次调用相比回溯小于等于 {@value MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY} ms的值
     * @implNote 之所以有该函数是为了允许 {@link Idg} 将获取时间偏移量的方式可以委托至非 {@link System#currentTimeMillis()} 方法，
     * 比如，在有NTP时间同步的集群中，可以委托至NTP服务
     */
    @FunctionalInterface
    public interface FnGenCurrTimeMillis {
        /**
         * 每次调用应返回当前时间(UTC)与 1970-01-01 00:00:00(UTC)之间的差值（以毫秒为单位）
         *
         * @return 毫秒数差值
         */
        long gen();
    }

    /**
     * 明确指定 machineId 的构造参数<br>
     * machineId {@value  #MACHINE_ID_BITS} 位，最多能区分 {@value MAX_MACHINE_ID_VALUE} 个
     *
     * @param machineId 同毫秒区分-机器码
     * @param fn        每次调用都能获取当前时间(UTC)与 1970-01-01 00:00:00(UTC)之间的差值（以毫秒为单位）的函数
     * @throws IllegalArgumentException 当给定 machineId 小于 0 或大于 {@value MAX_MACHINE_ID_VALUE} 时
     * @throws IllegalStateException    当当前系统调用 {@code System.currentTimeMillis()} 返回的毫秒值大于
     *                                  {@value EPOCH_END} 或 小于 {@value EPOCH_BEGIN} 时
     * @throws NullPointerException     当给定的函数为空时
     * @apiNote 在该 {@link Idg} 实例构造时便会调用给定的 {@link FnGenCurrTimeMillis} 函数校验当前时间合法性，
     * 遂请确定在构造该对象时给定的函数已可使用
     * @see #EPOCH_BEGIN
     * @see #EPOCH_END
     */
    public Idg(long machineId, FnGenCurrTimeMillis fn) {
        Objects.requireNonNull(fn, "fnGenCurrTimeMillis is null");
        long nowTimeMillis = fn.gen();
        if (nowTimeMillis < EPOCH_BEGIN || nowTimeMillis > EPOCH_END) {
            LocalDateTime now = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(nowTimeMillis), ZoneId.systemDefault());
            LocalDateTime bigBang = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(EPOCH_BEGIN), ZoneId.systemDefault());
            LocalDateTime deathOfTime = LocalDateTime
                    .ofInstant(Instant.ofEpochMilli(EPOCH_END), ZoneId.systemDefault());
            DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss:SSS");
            throw new IllegalStateException(STF.f("ID生成器初始化失败，不允许当前系统时间 '{}' 早于 '{}' 或晚于 '{}'",
                    now.format(dft), bigBang.format(dft), deathOfTime.format(dft)));
        }
        if (machineId > MAX_MACHINE_ID_VALUE || machineId < 0) {
            throw new IllegalArgumentException(STF.f("machineId > {} or < 0", MAX_MACHINE_ID_VALUE));
        }
        this.machineId = machineId;
        this.fnGenCurrTimeMillis = fn;
    }

    /**
     * 生成雪花ID<br>
     * 在同一毫秒内最多可生成 {@value #MAX_SEQUENCE_VALUE} 个雪花Id，
     * 若一个毫秒内请求数大于 {@value #MAX_SEQUENCE_VALUE} 则从第 {@value #MAX_SEQUENCE_VALUE} +1 个请求开始，
     * 请求所在的线程将会忙自旋到下一毫秒再从该方法返回雪花ID
     *
     * @return 雪花ID
     * @throws IllegalStateException 当时间发生至少两次连续回溯,
     *                               或时间发生一次大于 {@value #MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY} ms 的回溯时
     * @throws IllegalStateException 当线程收到中断信号时
     */
    public long nextId() {
        lock.lock();
        try {
            long timestamp = nowSystemTime();
            if (timestamp < lastTime) {  // 可能闰秒，可能发生大回溯
                long offset = lastTime - timestamp;
                if (offset <= MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY) {  // 差值小于设定可容忍值，等待
                    try {
                        TimeUnit.MILLISECONDS.sleep(offset << 1);  // 尝试休眠双倍差值后重新获取，再次校验
                        timestamp = nowSystemTime();
                        if (timestamp < lastTime) {  // 等待结束后，如果新获取的仍小于，则证明时间又发生回溯
                            throw new IllegalStateException(STF
                                    .f("系统时间发生回溯，拒绝在 {} 毫秒内生成Id", lastTime - timestamp));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 恢复中断信号，闰秒或极小回溯的线程休眠情况几乎不可能发生或被观测到
                        throw new IllegalStateException(e);
                    }
                } else { // 大的回溯，直接他妈异常
                    throw new IllegalStateException(STF.f("系统时间发生回溯，拒绝在 {} 毫秒内生成Id", offset));
                }
            }

            if (lastTime == timestamp) {
                // 相同毫秒内，序列号自增
                sequence = (sequence + 1) & MAX_SEQUENCE_VALUE;
                if (sequence == 0L) {
                    // 当进入该块，则表示毫秒数相同且序列号已达到最大, 线程则在该方法内忙自旋到至少下一毫秒才返回
                    timestamp = loop2NextTime(lastTime);
                }
            } else {
                sequence = 0L;
            }

            lastTime = timestamp;
            return buildId(timestamp, machineId, sequence);
        } finally {
            lock.unlock();
        }
    }

    private static long buildId(long genTimeMillis, long machineId, long sequence) {
        // 时间戳部分 | 机器标识部分 | 序列号部分
        return ((genTimeMillis - EPOCH_BEGIN) << TIMESTAMP_OFFSET)
               | (machineId << SEQUENCE_BITS)
               | sequence;
    }

    /**
     * 重置序列码并自旋，方法内当前线程会忙自旋到至少下一毫秒再返回<br>
     *
     * @return 新时间
     * @see #nowSystemTime()
     */
    private long loop2NextTime(long lastTimestamp) {
        long timestamp = nowSystemTime();
        while (timestamp <= lastTimestamp) { // 这里，一个毫秒内，线程将会忙到下一个毫秒才会返回
            timestamp = nowSystemTime();
        }
        return timestamp;
    }

    /**
     * 获取系统时间（当前时间(UTC)与 1970-01-01 00:00:00(UTC)之间的差值（以毫秒为单位））<br>
     */
    private long nowSystemTime() {
        /*
        原有这里使用 ScheduledExecutorService 在高并发情境下优化 System.currentTimeMillis 的性能问题，这里省去了该过程，
        20250524：现已改为方法引用
         */
        return this.fnGenCurrTimeMillis.gen();
    }

    /**
     * 取 mac 地址后 16 位<br>
     *
     * @return mac地址后16位
     * @apiNote 当无网卡时，返回 {@code 1}，
     * 当获取网卡地址失败时，返回 {@code 0}
     */
    private static long macLast16bit() {
        long macLast16Bit = 0L;
        try {
            // 拿个网卡MAC地址后两字节值
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(address);
            if (null == network) {
                macLast16Bit = 1L; // 无网卡
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    macLast16Bit = (0x00FFL & (long) mac[mac.length - 2]) | (0xFF00L & (((long) mac[mac.length - 1]) << 8));
                }
            }
        } catch (Exception ignored) {
            // ignored err ,return 0L
        }
        return macLast16Bit;
    }

    /**
     * 取 进程号和电脑名 后 {@value #MACHINE_ID_BITS} 位<br>
     *
     * @return machineId
     */
    private static long machineId10Bit() {
        // 获取 pid+电脑名
        String pidAndCptName = ManagementFactory.getRuntimeMXBean().getName();
        return Objects.hash(pidAndCptName.hashCode()) & MAX_MACHINE_ID_VALUE;
    }

    /**
     * {@link Idg}生成的Id结构记录
     *
     * @param genTimeMillis 生成时间
     * @param machineId     机器码
     * @param sequence      序列号
     */
    public record Id(long genTimeMillis, long machineId, long sequence) {
        // 该不应作为生成id过程的中间对象，否则高并发情况下会创建大量内存垃圾

        /**
         * 接收一个{@link Idg#nextId()}生成的Id，返回该Id的结构记录
         *
         * @param snowflakeId 雪花Id
         * @return Id结构记录
         */
        public static Id ofLongId(long snowflakeId) {
            long genTimeMillis = (snowflakeId >> TIMESTAMP_OFFSET) + EPOCH_BEGIN;
            long machineId = (~(-1 << MACHINE_ID_BITS)) & (snowflakeId >> SEQUENCE_BITS);
            long sequence = (~(-1 << SEQUENCE_BITS)) & snowflakeId;
            return new Id(genTimeMillis, machineId, sequence);
        }

        public long toLongId() {
            return buildId(genTimeMillis, machineId, sequence);
        }
    }
}
