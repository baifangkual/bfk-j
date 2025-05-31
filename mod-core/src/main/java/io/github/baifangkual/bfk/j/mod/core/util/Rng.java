package io.github.baifangkual.bfk.j.mod.core.util;


import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * <b>Random Number Generator</b><br>
 * 伪随机数生成器，默认的随机数生成需求会委托至 {@link ThreadLocalRandom#current()}，
 * 遂该类中方法线程安全且不会发生竞态<br>
 *
 * @author baifangkual
 * @see #nextFixLenLarge(int)
 * @see RandomGenerator
 * @see ThreadLocalRandom
 * @see Math#random()
 * @see java.util.Random
 * @see java.util.random.RandomGeneratorFactory
 * @since 2024/12/11 v0.0.3
 */
public final class Rng {

    /**
     * 返回一个伪随机选择的 大数 值，一定不为负数<br>
     * <pre>
     *     {@code
     *     int largeNumLength = 100;
     *     String largeNum = Rng.nextFixLenLarge(largeNumLength);
     *     Assert.isTrue(largeNum.length() == largeNumLength);
     *     Assert.gtOrEq(Long.valueOf(Rng.nextFixLenLarge(1)), 0L);
     *     Assert.lt(Long.valueOf(Rng.nextFixLenLarge(1)), 10L);
     *     Assert.throwException(NumberFormatException.class, () -> Long.valueOf(Rng.nextFixLenLarge(1000)));
     *     }
     * </pre>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     */
    public static String nextFixLenLarge(int length) {
        Err.realIf(length < 1, IllegalArgumentException::new, "Length must > 0");
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (length == 1) {
            return String.valueOf(rng.nextInt(10));
        } else {
            StringBuilder n = new StringBuilder();
            int len = n.length();
            while (len != length) {
                if (len < length) { // 小了补
                    n.append(rng.nextLong(1, Long.MAX_VALUE));
                } else { // 多了削
                    n.delete(length, Integer.MAX_VALUE);
                }
                len = n.length();
            }
            return n.toString();
        }
    }

    /**
     * 返回一个伪随机选择的 大数 值，一定不为负数（值字面量为指定进制的字面量）<br>
     *
     * @param radix  进制
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @throws IllegalArgumentException 给定的进制值小于2或大于62
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     * @see Radixc
     */
    public static String nextFixLenLarge(int length, int radix) {
        int radixNumLen = Radixc.base2len(length, 10, radix);
        return Radixc.convert(nextFixLenLarge(radixNumLen), 10, radix);
    }

    /**
     * 返回一个伪随机选择的 {@link BigInteger}<br>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @since v0.0.7
     */
    public static BigInteger nextFixLenBigInt(int length) {
        return new BigInteger(nextFixLenLarge(length));
    }

    /**
     * 返回一个伪随机选择的 long 值
     */
    public static long nextLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [origin, bound)}
     * <pre>
     *     {@code
     *     double rd = Rng.nextLong(origin, bound);
     *     Assert.gtOrEq(rd, origin);
     *     Assert.lt(rd, bound);
     *     }
     * </pre>
     *
     * @throws IllegalArgumentException 如果 bound 不是正数
     */
    public static long nextLong(long origin, long bound) {
        return ThreadLocalRandom.current().nextLong(origin, bound);
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [0, bound)}
     * <pre>
     *     {@code
     *     double rd = Rng.nextLong(bound);
     *     Assert.gtOrEq(rd, 0);
     *     Assert.lt(rd, bound);
     *     }
     * </pre>
     */
    public static long nextLong(long bound) {
        return ThreadLocalRandom.current().nextLong(bound);
    }


    /**
     * 返回一个伪随机选择的 boolean 值
     */
    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * 返回一个伪随机选择的 double 值，区间为 {@code [0, 1)}
     * <pre>
     *     {@code
     *     double rd = Rng.nextDouble();
     *     Assert.gtOrEq(rd, 0);
     *     Assert.lt(rd, 1);
     *     }
     * </pre>
     */
    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * 返回一个伪随机选择的 int 值，区间为 {@code [0, bound)}
     */
    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * 返回一个伪随机选择的 int 值，区间为 {@code [origin, bound)}
     */
    public static int nextInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    /**
     * 返回一个伪随机选择的 int 值
     */
    public static int nextInt() {
        return ThreadLocalRandom.current().nextInt();
    }


}
