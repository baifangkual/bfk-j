package io.github.baifangkual.bfk.j.mod.core.util;


import io.github.baifangkual.bfk.j.mod.core.lang.Const;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * <b>Random Number Generator</b><br>
 * 伪随机数生成器，默认的随机数生成需求会委托至 {@link ThreadLocalRandom#current()}，
 * 遂该类中方法线程安全且不会发生竞态<br>
 *
 * @author baifangkual
 * @see #rollFixLenLarge(int)
 * @see RandomGenerator
 * @see ThreadLocalRandom
 * @see Math#random()
 * @see java.util.Random
 * @see java.util.random.RandomGeneratorFactory
 * @since 2024/12/11 v0.0.3
 */
public final class Rng {

    /**
     * 返回一个伪随机选择的 大数 值<br>
     * <pre>
     *     {@code
     *     int largeNumLength = 100;
     *     String largeNum = Rng.rollFixLenLarge(largeNumLength);
     *     Assert.isTrue(largeNum.length() == largeNumLength);
     *     Assert.gtOrEq(Long.valueOf(Rng.rollFixLenLarge(1)), 0L);
     *     Assert.lt(Long.valueOf(Rng.rollFixLenLarge(1)), 10L);
     *     Assert.throwException(NumberFormatException.class, () -> Long.valueOf(Rng.rollFixLenLarge(1000)));
     *     }
     * </pre>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     */
    public static String rollFixLenLarge(int length) {
        Err.realIf(length < 1, IllegalArgumentException::new, "Length must > 0");
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (length == 1) {
            return Const.BASE62_CHARS_LOOKUP_TABLE.get(rng.nextInt(10)); //只要一个那就直接给，不创建str
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
     * 返回一个伪随机选择的 long 值
     */
    public static long rollLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [origin, bound)}
     * <pre>
     *     {@code
     *     double rd = Rng.rollLong(origin, bound);
     *     Assert.gtOrEq(rd, origin);
     *     Assert.lt(rd, bound);
     *     }
     * </pre>
     *
     * @throws IllegalArgumentException 如果 bound 不是正数
     */
    public static long rollLong(long origin, long bound) {
        return ThreadLocalRandom.current().nextLong(origin, bound);
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [0, bound)}
     * <pre>
     *     {@code
     *     double rd = Rng.rollLong(bound);
     *     Assert.gtOrEq(rd, 0);
     *     Assert.lt(rd, bound);
     *     }
     * </pre>
     */
    public static long rollLong(long bound) {
        return ThreadLocalRandom.current().nextLong(bound);
    }


    /**
     * 返回一个伪随机选择的 boolean 值
     */
    public static boolean rollBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * 返回一个伪随机选择的 double 值，区间为 {@code [0, 1)}
     * <pre>
     *     {@code
     *     double rd = Rng.rollDouble();
     *     Assert.gtOrEq(rd, 0);
     *     Assert.lt(rd, 1);
     *     }
     * </pre>
     */
    public static double rollDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }


}
