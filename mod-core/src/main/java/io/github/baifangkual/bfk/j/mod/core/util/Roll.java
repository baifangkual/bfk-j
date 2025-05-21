package io.github.baifangkual.bfk.j.mod.core.util;


import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>投骰子</b><br>
 * 随机数工具类，可生成雪花id、随机数等
 *
 * @author baifangkual
 * @see #fixedLengthNumber(int)
 * @see #nextId()
 * @see #nextFixedLengthNumberId(int)
 * @since 2024/12/11 v0.0.3
 */
public final class Roll {
    private Roll() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 给定一个值表示长度(数字字符个数)，返回一个数字字符串<br>
     * 该数字取随机数，该数字的数字字符个数为给定的长度值, 返回的数字字符串一定为正整数，一定不以 0 开头<br>
     * 该方法线程安全<br>
     * {@code Assert.eq(fixedLengthNumber(100).length(), fixedLengthNumber(100).length())}<br>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型<br>
     * 另外，该方法返回的数字仅表示固定位数的随机数，不能保证在两次调用一定获取到不同的值，遂该方法的返回值不能作为唯一索引<br>
     */
    public static String fixedLengthNumber(int length) {
        Err.realIf(length < 1, IllegalArgumentException::new, "Length must be max 0");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder n = new StringBuilder();
        int len = n.length();
        while (len != length) {
            if (len < length) { // 小了补
                n.append(random.nextLong(1, Long.MAX_VALUE));
            } else { // 多了削
                n.delete(length, Integer.MAX_VALUE);
            }
            len = n.length();
        }
        return n.toString();
    }

    /**
     * 给定一个值表示长度(数字个数)，尝试返回一个数字<br>
     * 该数字取随机数，该数字的个数为给定的长度值, 若成功，则返回的数字一定为正整数，一定不以 0 开头<br>
     * 若给定的长度参数小于1，或长度参数过大（无法用Long类型表达）则返回R.Err，该方法线程安全<br>
     *
     * @param length 数字字符个数
     * @return R.Ok(LargeNumber) | R.Err(RuntimeException)
     * @see #fixedLengthNumber(int)
     */
    public static R<Long, RuntimeException> tryFixedLengthNumber(int length) {
        return R.ofSupplier(() -> Long.valueOf(fixedLengthNumber(length)));
    }

    private static final IdGenerator ID_GEN = new IdGenerator();

    /**
     * 返回一个雪花id<br>
     * 生成64位长正整数雪花id，该方法可保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复<br>
     * 该方法线程安全<br>
     *
     * @return 雪花id
     * @throws IllegalStateException 当时间发生至少两次连续回溯,
     *                               或时间发生一次大于{@link IdGenerator#MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY}ms的回溯时
     * @throws RuntimeException      当线程收到中断信号时
     * @apiNote 该方法依赖的底层方法在高并发情况下会频繁进行系统调用
     * @see #nextFixedLengthNumberId(int)
     * @see IdGenerator#nextId()
     */
    public static long nextId() {
        return ID_GEN.nextId();
    }

    /**
     * 给定一个数字，表示所需要的id的字符长度，返回一个id值，该id值字符长度为要求的长度<br>
     * 该方法线程安全<br>
     *
     * @param length 要求的字符长度
     * @return 返回仅数字字符的字符串，字符串字符数一定等于要求的长度
     * @throws IllegalArgumentException 当给定的参数小于一个64位雪花id表达的字符串的长度时
     * @apiNote 该id值的唯一性保证与雪花id同理，即保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复，
     * 另外，当给定的length值过小时，该方法将抛出异常，因为为确保唯一性，生成的id字符长度至少要大于当前雪花id的字符长度，
     * 建议给定的值大于64位有符号数表达的最大值的字符长度数(19)<br>
     * 需注意，该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     * @see #nextId()
     */
    public static String nextFixedLengthNumberId(int length) {
        String sfId = String.valueOf(nextId());
        int sfIdl = sfId.length();
        if (sfIdl > length) {
            throw new IllegalArgumentException("非法参数，给定的length过小，甚至无法表达雪花id的字符长度");
        } else if (sfIdl == length) {
            return sfId;
        }
        return sfId + fixedLengthNumber(length - sfIdl);
    }


    /**
     * 雪花id生成器<br>
     * 生成的雪花id共 63 位有效位，符号位永远为 0<br>
     * 其中 41 位为时间偏移量，{@value MACHINE_ID_BITS} 位为 machineId 标识，{@value CENTER_ID_BITS} 位为 centerId 标识，
     * {@value SEQUENCE_BITS} 位为同毫秒细分的序列号<br>
     * 该Id构成: {@code 0(符号位) - 时间偏移量 - 数据中心标识 - 机器码 - 同毫秒序列}<br>
     * 该实例构造可自定 centerId 和 machineId 标识，因为该标识为自定量，遂生成的id序列中携带了自定量信息<br>
     * 该工具类对时间回溯没有太大抗性，当两次通过 {@link #nextId()} 获取id的间隔中发生多次
     * 或单次大于 {@value #MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY} 毫秒的时间回溯时，该方法将抛出运行时异常<br>
     * <p>
     * 该算法及代码参考：
     * <a href="https://zh.wikipedia.org/wiki/%E9%9B%AA%E8%8A%B1%E7%AE%97%E6%B3%95">雪花算法</a>
     * <a href="https://gitee.com/yu120/sequence/blob/master/src/main/java/cn/ms/sequence/Sequence.java">分布式高效ID生产黑科技</a>
     * <a href="https://github.com/beyondfengyu/SnowFlake">SnowFlake</a>
     *
     * @author baifangkual
     * @since 2024/12/12 v0.0.3
     */
    private static final class IdGenerator {

        // DEFINITION ==================================
        /**
         * 时间起始标记点，作为基准，任何情况不应变动该值<br>
         * 当前设定的时间戳值 1609459200000L 表示时间 2021-01-01 00:00:00，
         * {@code 63 - SEQUENCE_BITS - MACHINE_ID_BITS - CENTER_ID_BITS} (41) 位为该雪花id时间戳最大位数，
         * 遂当前实例可用到 2090-09-07 23:47:35
         */
        private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
        /**
         * 数据中心区分，占多少bit，数据中心个数最多 {@value MAX_CENTER_ID_VALUE} 个
         */
        private static final long CENTER_ID_BITS = 5L;
        /**
         * 机器码区分，占多少bit，机器最多区分 {@value MAX_MACHINE_ID_VALUE} 个
         */
        private static final long MACHINE_ID_BITS = 7L;
        /**
         * 同毫秒区分-序列码区分，占多少bit，每毫秒内可用的id数: {@value MAX_SEQUENCE_VALUE} 个
         */
        private static final long SEQUENCE_BITS = 10L;
        /**
         * 最大的容忍回溯和闰秒的毫秒数，该值不宜过大，因为对回溯的抵抗将由线程的TIME-SLEEP负责
         */
        private static final long MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY = 5L;

        // 最大值
        private static final long MAX_CENTER_ID_VALUE = ~(-1L << CENTER_ID_BITS);
        private static final long MAX_MACHINE_ID_VALUE = ~(-1L << MACHINE_ID_BITS);
        private static final long MAX_SEQUENCE_VALUE = ~(-1L << SEQUENCE_BITS);

        // bit 偏移量
        private static final long MACHINE_ID_OFFSET = MACHINE_ID_BITS + SEQUENCE_BITS;
        private static final long TIMESTAMP_OFFSET = CENTER_ID_BITS + MACHINE_ID_BITS + SEQUENCE_BITS;

        // INSTANCE ====================================
        private final Lock LOCK = new ReentrantLock();
        // 当毫秒数相同且序列号消耗完的await
        private final Condition AWAIT = LOCK.newCondition();
        // 区分01-数据中心
        private final long centerId;
        // 区分02-机器码
        private final long machineId;

        // MUTABLE =====================================
        // 序列号原子变量
        private final AtomicLong sequence = new AtomicLong(0L);
        // 上次生产 ID 时间戳, 初始值设定为 -1L
        private volatile long lastSystemTime = -1L;

        /**
         * centerId 使用 mac地址后10位余数，machineId 使用当前计算机名+进程pid哈希后16位余数<br>
         * machineId {@value  #MACHINE_ID_BITS} 位，centerId 应当占 {@value  #CENTER_ID_BITS} 位
         */
        public IdGenerator() {
            this(defaultMachineId(), defaultCenterId());
        }

        /**
         * 明确指定 区分 machineId 和 centerId 的构造参数<br>
         * machineId {@value  #MACHINE_ID_BITS} 位，centerId 应当占 {@value  #CENTER_ID_BITS} 位
         *
         * @param machineId 同毫秒区分-机器码
         * @param centerId  同毫秒区分-数据中心Id
         */
        public IdGenerator(long machineId, long centerId) {
            if (machineId > MAX_MACHINE_ID_VALUE || machineId < 0) {
                throw new IllegalArgumentException(String.format("machineId > %d or < 0", MAX_MACHINE_ID_VALUE));
            }
            if (centerId > MAX_CENTER_ID_VALUE || centerId < 0) {
                throw new IllegalArgumentException(String.format("centerId > %d or < 0", MAX_CENTER_ID_VALUE));
            }
            this.machineId = machineId;
            this.centerId = centerId;
        }

        /**
         * 生成下一个雪花id<br>
         * 该方法线程安全，且保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复<br>
         * 在同一毫秒内最多可生成{@value #MAX_SEQUENCE_VALUE}个雪花Id，
         * 若一个毫秒内请求数大于{@value #MAX_SEQUENCE_VALUE}则从第{@value #MAX_SEQUENCE_VALUE}+1个请求开始，
         * 请求所在的线程将会忙自旋到下一毫秒再从该方法返回雪花id
         *
         * @return 雪花id
         * @throws IllegalStateException 当时间发生至少两次连续回溯,
         *                               或时间发生一次大于{@value #MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY}ms的回溯时
         * @throws RuntimeException      当线程收到中断信号时
         * @apiNote 该方法依赖的底层方法在高并发情况下会频繁进行系统调用
         */
        public long nextId() {
            LOCK.lock();
            try {
                long timestamp = nowSystemTime();
                // 可能闰秒，可能发生大回溯
                if (timestamp < lastSystemTime) {
                    long offset = lastSystemTime - timestamp;
                    // 差值过小，可能闰秒，则等待
                    if (offset <= MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY) {
                        try {
                            // 尝试休眠双倍差值后重新获取，再次校验
                            if (!AWAIT.await(offset << 1, TimeUnit.MILLISECONDS)) {
                                // 因为其他地方不会操纵CONDITION唤醒当前线程，遂该if块内代码一定不会发生，该内代码仅为屏蔽idea烦人提示
                                throw new IllegalStateException("pass ... signal");
                            }
                            timestamp = nowSystemTime();
                            // 等待结束后，如果新获取的仍小于，则证明时间又发生回溯，玩你妈，应抛出异常
                            if (timestamp < lastSystemTime) {
                                throw new IllegalStateException(String
                                        .format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastSystemTime - timestamp));
                            }
                        } catch (InterruptedException e) {
                            // 恢复中断信号，因为闰秒或极小的时间回溯导致的休眠情况几乎不可能发生或在正常情况下被观测到，遂线程中断信号几乎不会在当前代码段内导致异常中断
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread InterruptedException");
                        }
                    } else {
                        // 大的回溯，直接他妈异常，玩你妈
                        throw new IllegalStateException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                }

                long seq;

                if (lastSystemTime == timestamp) {
                    // 相同毫秒内，序列号自增
                    seq = sequence.incrementAndGet() & MAX_SEQUENCE_VALUE;
                    if (seq == 0L) {
                        // 当进入该块，则表示毫秒数相同且序列号已达到最大, 线程则在该方法内类似忙自旋般等到至少下一毫秒才返回
                        timestamp = resetSeqAndLoop2NextTime(lastSystemTime);
                    }
                } else {
                    // 不同毫秒内，序列号置为 1 - 3 随机数
                    // to do perf，该set移动到loop2NextTime内，这样每次不同毫秒的请求就不用重新set
                    //   也无需ThreadLocalRandom.current().nextLong(1, 3)，每次set0即可，已细想过不会重复
                    //  因为上sequence.incrementAndGet()会自增，遂当同一毫秒，sequence.set0之后的第二次，至少都是拿到seq1
                    // no need perf
                    // sequence.set(ThreadLocalRandom.current().nextLong(1, 3));
                    seq = sequence.get();
                }

                lastSystemTime = timestamp;

                // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
                return ((timestamp - EPOCH) << TIMESTAMP_OFFSET)
                       | (centerId << MACHINE_ID_OFFSET)
                       | (machineId << SEQUENCE_BITS)
                       | seq;
            } finally {
                LOCK.unlock();
            }
        }

        /**
         * 重置序列码并自旋，方法内当前线程会忙自旋到至少下一毫秒再返回<br>
         *
         * @return 新时间
         * @see #nowSystemTime()
         */
        private long resetSeqAndLoop2NextTime(long lastTimestamp) {
            sequence.set(0); // no need perf
            long timestamp = nowSystemTime();
            while (timestamp <= lastTimestamp) { // 这里，一个毫秒内，线程将会忙到下一个毫秒才会返回
                timestamp = nowSystemTime();
            }
            return timestamp;
        }

        /**
         * 获取系统时间<br>
         *
         * @return 系统时间
         * @apiNote 该方法在高并发情况下会频繁进行系统调用，
         * 遂该方法在高并发情况下使用并不好，高并发情况下或应使用ScheduledExecutorService缓存时间戳值
         */
        private long nowSystemTime() {
            /*
            原有这里使用 ScheduledExecutorService 在高并发情境下优化 System.currentTimeMillis 的性能问题，这里省去了该过程
             */
            return System.currentTimeMillis();
        }

        /**
         * 通过mac地址后10位构建 centerId<br>
         * 占用 {@value #CENTER_ID_BITS} 位
         *
         * @return centerId
         * @apiNote 当无网卡时，返回 {@code 1}，
         * 当获取网卡地址失败时，返回 {@value #MAX_CENTER_ID_VALUE}
         */
        private static long defaultCenterId() {
            long id = 0L;
            try {
                // 拿个网卡MAC地址后两字节值
                InetAddress address = InetAddress.getLocalHost();
                NetworkInterface network = NetworkInterface.getByInetAddress(address);
                if (null == network) {
                    id = 1L; // 无网卡
                } else {
                    byte[] mac = network.getHardwareAddress();
                    if (null != mac) {
                        id = ((0x00FFL & (long) mac[mac.length - 2]) | (0xFF00L & (((long) mac[mac.length - 1]) << 8))) >> 6;
                        id = id % (MAX_CENTER_ID_VALUE + 1);
                    }
                }
            } catch (Exception e) {
                //throw new IllegalStateException(e.getMessage(), e);
                id = MAX_CENTER_ID_VALUE; // 不抛出异常, 直接给最大值
            }
            return id;
        }

        /**
         * 通过进程号和电脑名构建 machineId<br>
         * 占用 {@value #MACHINE_ID_BITS} 位
         *
         * @return machineId
         */
        private static long defaultMachineId() {
            // 获取 pid+电脑名
            String pidAndCptName = ManagementFactory.getRuntimeMXBean().getName();
            return (pidAndCptName.hashCode() & 0xffff) % (MAX_MACHINE_ID_VALUE + 1);
        }

    }
}
