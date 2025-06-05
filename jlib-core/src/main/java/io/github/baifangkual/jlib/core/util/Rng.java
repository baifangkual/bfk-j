package io.github.baifangkual.jlib.core.util;


import io.github.baifangkual.jlib.core.panic.Err;

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
        // 当length等于1时，可能随机到0，尤其二进制下，随机到0概率百分之50
        if (length == 1) {
            // 随机一个10进制下 0 - 进制值（取不到）之间的数（可能在10进制下是多位，再将其转为相应进制
            return Radixc.convert(Rng.nextLong(0, radix), radix);
        }
        // 当length大于1时，不会随机到0，可放心转换
        if (length > 1) {
            // 反向，一直要求进制下的长度，求在10进制下最小能覆盖到所有数的长度
            int radix10NumLen = Radixc.base2len(length, radix, 10);
            // 因为为覆盖要求进制下的所有数，所以10进制下要求长度的值转为指定进制可能大于
            // 要求的长度，遂剪掉长度
            String fixLenNum = nextFixLenLarge(radix10NumLen);
            StringBuilder largeAfterRadixConvert = new StringBuilder(Radixc.convert(fixLenNum, 10, radix));
            int afterCvLen = largeAfterRadixConvert.length();
            // fix：因为base2len算出来的是一定覆盖到的最小长度
            // 当Large出来的10进制随机数过小时，转为低进制的可能会不够长度，需要补
            // 经测试在不大的数时仅会相差1位，但数字超大时，可能相差多位，遂这里需循环
            while (afterCvLen < length) {
                largeAfterRadixConvert.append(nextFixLenLarge(length - afterCvLen, radix));
                afterCvLen = largeAfterRadixConvert.length();
            }
            if (afterCvLen > length) {
                largeAfterRadixConvert.delete(length, Integer.MAX_VALUE);
            }

            return largeAfterRadixConvert.toString();
        }
        throw new IllegalArgumentException("Length must > 0");
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
