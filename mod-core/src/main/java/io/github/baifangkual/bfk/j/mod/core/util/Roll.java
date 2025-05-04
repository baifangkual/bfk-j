package io.github.baifangkual.bfk.j.mod.core.util;


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
 * 投骰子，随机数工具
 *
 * @author baifangkual
 * @since 2024/12/11
 */
public final class Roll {
    private Roll() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 给定一个值表示长度(位数)，返回一个数字，该数字取随机数，该数字的位数为给定的长度值, 返回的数字字符串一定为正整数<br>
     * 需注意，该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型<br>
     * 需注意，该方法返回的数字仅表示固定位数的随机数，不能保证在两次调用一定获取到不同的值，遂该方法的返回值不能作为唯一索引<br>
     * 该方法返回的数字一定不以 0 开头<br>
     * 如 {@code “54321”.length() == String.valueOf(fixedLengthNumber(5)).length()}
     */
    public static String fixedLengthNumber(int length) {
        Err.realIf(length < 1, IllegalArgumentException::new, "Length must be max 0");
        double random = Math.random();
        StringBuilder n = new StringBuilder(String.valueOf(random).substring(2));
        int len = n.length();
        while (len != length) {
            if (len < length) { // 小了补
                n.append(fixedLengthNumber(length - len));
                len = n.length();
            } else { // 多了削
                n = new StringBuilder(random > 0.5 ? n.substring(len - length) : n.substring(0, length));
                len = n.length();
            }
        }
        // 不允许以0开头
        if (n.indexOf("0") == 0) {
            int noZeroHeader = ((int) (random * 100)) % 9 + 1; // 找一个随机的非零开头
            n.replace(0, 1, String.valueOf(noZeroHeader));
        }
        return n.toString();
    }

    private static final IdGenerator ID_GEN = new IdGenerator();

    /**
     * 生成64位长正整数雪花id，该方法可保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复
     */
    public static long nextId() {
        return ID_GEN.nextId();
    }

    /**
     * 给定一个数字，表示所需要的id的字符长度，返回一个id值，该id值字符长度为要求的长度<br>
     * 该id值的唯一性保证与雪花id同理，即保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复，
     * 另外，当给定的length值过小时，该方法将抛出异常，因为为确保唯一性，生成的id字符长度至少要大于当前雪花id的字符长度，
     * 建议给定的值大于64位有符号数表达的最大值的字符长度数(19)
     * 需注意，该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型<br>
     *
     * @param length 要求的字符长度
     * @return 返回仅数字字符的字符串，字符串字符数一定等于要求的长度
     * @throws IllegalArgumentException 当给定的参数小于一个64位雪花id表达的字符串的长度时
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
     * 生成的雪花id共63位有效位，符号位永远为0<br>
     * 其中41位为时间偏移量，8位为man标识，4位为home标识，10位为同毫秒细分的序列号<br>
     * 该实例构造可自定man+home 12位的标识，因为该12位标识为自定量，遂生成的id序列中携带了自定量信息<br>
     * 该工具类对时间回溯没有太大抗性，当两次通过{@link #nextId()}获取id的间隔中发生多次
     * 或单次大于{@link #MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY}值的时间回溯时，该方法将抛出运行时异常<br>
     * 该算法及代码参考：
     * <ul>
     *     <li><a href="https://zh.wikipedia.org/wiki/%E9%9B%AA%E8%8A%B1%E7%AE%97%E6%B3%95">雪花算法</a></li>
     *     <li><a href="https://gitee.com/yu120/sequence/blob/master/src/main/java/cn/ms/sequence/Sequence.java">分布式高效ID生产黑科技</a></li>
     * </ul>
     *
     * @author baifangkual
     * @since 2024/12/12
     */
    public static final class IdGenerator {

        // DEFINITION ==================================
        /**
         * 时间起始标记点，作为基准，任何情况不应变动该值<br>
         * 当前设定的时间戳值1609459200000L表示时间 2021-01-01 00:00:00，41位为该雪花id时间戳最大位数，遂当前实例可用到 2090-09-07 23:47:35
         */
        private static final long EPOCH = 1609459200000L; // 2021-01-01 00:00:00
        /**
         * 同毫秒区分01-数据中心区分，最多五位，数据中心个数最多 2^4 个
         */
        private static final long HOME_ID_BITS = 4L;
        /**
         * 同毫秒区分02-机器码区分，最多五位，机器最多区分 2^8 个
         */
        private static final long MAN_ID_BITS = 8L;
        /**
         * 同毫秒区分03-序列码区分，最多12位，每毫秒内可用的id数: 2^10
         */
        private static final long SEQUENCE_BITS = 10L;
        /**
         * 最大的容忍回溯和闰秒的毫秒数，该值不宜过大，因为对回溯的抵抗将由线程的TIME-SLEEP负责
         */
        private static final long MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY = 5L;

        // 最大值
        private static final long MAX_HOME_ID_VALUE = ~(-1L << HOME_ID_BITS);
        private static final long MAX_MAN_ID_VALUE = ~(-1L << MAN_ID_BITS);
        private static final long MAX_SEQUENCE_VALUE = ~(-1L << SEQUENCE_BITS);

        // bit 偏移量
        private static final long MAN_ID_OFFSET = MAN_ID_BITS + SEQUENCE_BITS;
        private static final long TIMESTAMP_OFFSET = HOME_ID_BITS + MAN_ID_BITS + SEQUENCE_BITS;

        // INSTANCE ====================================
        private final Lock LOCK = new ReentrantLock();
        // 当毫秒数相同且序列号消耗完的await
        private final Condition AWAIT = LOCK.newCondition();
        // 区分01-数据中心
        private final long home;
        // 区分02-机器码
        private final long man;

        // MUTABLE =====================================
        // 序列号原子变量
        private final AtomicLong sequence = new AtomicLong(0L);
        // 上次生产 ID 时间戳, 初始值设定为 -1L
        private volatile long lastTimestamp = -1L;

        /**
         * home使用 mac地址后10位余数，man使用当前计算机名+进程pid哈希后16位余数
         */
        public IdGenerator() {
            this(createManId(), createHomeId());
        }

        /**
         * 明确指定 区分 man 和 home 的构造参数, man应当占{@link #MAN_ID_BITS}位，home应当占{@link #HOME_ID_BITS}位
         *
         * @param man  区分方式02，man！what can i say？
         * @param home 区分方式01，which home？
         */
        public IdGenerator(long man, long home) {
            if (man > MAX_MAN_ID_VALUE || man < 0) {
                throw new IllegalArgumentException(String.format("非法参数：man id 大于 %d 或小于 0", MAX_MAN_ID_VALUE));
            }
            if (home > MAX_HOME_ID_VALUE || home < 0) {
                throw new IllegalArgumentException(String.format("非法参数：home id 大于 %d 或小于 0", MAX_HOME_ID_VALUE));
            }
            this.man = man;
            this.home = home;
        }

        /**
         * 生成下一个雪花id，该方法可保证在不发生时间回溯的情况下在同一台设备获取到的id一定不重复
         */
        public long nextId() {
            LOCK.lock();
            try {
                long timestamp = systemTimeGen();
                // 可能闰秒，可能发生大回溯
                if (timestamp < lastTimestamp) {
                    long offset = lastTimestamp - timestamp;
                    // 差值过小，可能闰秒，则等待
                    if (offset <= MAX_FAULT_TOLERANT_BACKTRACKING_CAPACITY) {
                        try {
                            // 尝试休眠双倍差值后重新获取，再次校验
                            if (!AWAIT.await(offset << 1, TimeUnit.MILLISECONDS)) {
                                // 因为其他地方不会操纵CONDITION唤醒当前线程，遂该if块内代码一定不会发生，该内代码仅为屏蔽idea烦人提示
                                throw new IllegalStateException("pass ... signal");
                            }
                            timestamp = systemTimeGen();
                            // 等待结束后，如果新获取的仍小于，则证明时间又发生回溯，玩你妈，应抛出异常
                            if (timestamp < lastTimestamp) {
                                throw new IllegalStateException(String
                                        .format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
                            }
                        } catch (InterruptedException e) {
                            // 恢复中断信号，因为闰秒或极小的时间回溯导致的休眠情况几乎不可能发生或在正常情况下被观测到，遂线程中断信号几乎不会在当前代码段内导致异常中断
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Thread InterruptedException");
                        }
                    } else {
                        // 大的回溯，直接他妈异常，玩你妈
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                }

                long seq;

                if (lastTimestamp == timestamp) {
                    // 相同毫秒内，序列号自增
                    seq = sequence.incrementAndGet() & MAX_SEQUENCE_VALUE;
                    if (seq == 0L) {
                        // 当进入该块，则表示毫秒数相同且序列号已达到最大, 线程则在该方法内类似忙自旋般等到至少下一毫秒才返回
                        timestamp = loop2NextMillis(lastTimestamp);
                    }
                } else {
                    // 不同毫秒内，序列号置为 1 - 3 随机数
                    sequence.set(ThreadLocalRandom.current().nextLong(1, 3));
                    seq = sequence.get();
                }

                lastTimestamp = timestamp;

                // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
                return ((timestamp - EPOCH) << TIMESTAMP_OFFSET)
                       | (home << MAN_ID_OFFSET)
                       | (man << SEQUENCE_BITS)
                       | seq;
            } finally {
                LOCK.unlock();
            }
        }

        /**
         * 通过mac地址后10位构建 home id
         */
        private static long createHomeId() {
            long id = 0L;
            try {
                // 拿个网卡MAC地址后两字节值
                InetAddress address = InetAddress.getLocalHost();
                NetworkInterface network = NetworkInterface.getByInetAddress(address);
                if (null == network) {
                    id = 1L;
                } else {
                    byte[] mac = network.getHardwareAddress();
                    if (null != mac) {
                        id = ((0x00FFL & (long) mac[mac.length - 2]) | (0xFF00L & (((long) mac[mac.length - 1]) << 8))) >> 6;
                        id = id % (MAX_HOME_ID_VALUE + 1);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("fail getDataCenterId, err: " + e.getMessage(), e);
            }
            return id;
        }

        /**
         * 通过进程号和电脑名构建 man id
         */
        private static long createManId() {
            // 获取 pid+电脑名
            String pidAndCptName = ManagementFactory.getRuntimeMXBean().getName();
            return (pidAndCptName.hashCode() & 0xffff) % (MAX_MAN_ID_VALUE + 1);
        }


        /**
         * 方法内忙自旋到至少下一毫秒再返回
         */
        private long loop2NextMillis(long lastTimestamp) {
            long timestamp = systemTimeGen();
            while (timestamp <= lastTimestamp) { // 这里，一个毫秒内，线程将会忙到下一个毫秒才会返回
                timestamp = systemTimeGen();
            }
            return timestamp;
        }

        /**
         * 获取系统时间，注意，该方法在高并发情况下使用并不好，高并发情况下或应使用ScheduledExecutorService缓存时间戳值
         */
        private long systemTimeGen() {
            /*
            原有这里使用 ScheduledExecutorService 在高并发情境下优化 System.currentTimeMillis 的性能问题，这里省去了该过程
             */
            return System.currentTimeMillis();
        }

    }
}
