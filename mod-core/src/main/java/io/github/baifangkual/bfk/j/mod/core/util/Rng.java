package io.github.baifangkual.bfk.j.mod.core.util;


import io.github.baifangkual.bfk.j.mod.core.lang.Const;
import io.github.baifangkual.bfk.j.mod.core.mark.Default;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * <b>Random Number Generator</b><br>
 * 该类内各项保证均依赖于内部 {@link RandomGenerator} 实例的对应保证，且有：
 * <ul>
 *     <li>{@link #defaultSingle()} 的该实例内部 {@link RandomGenerator} 为 {@link ThreadLocalRandom}，
 *     线程安全，但可预测（在加密上不安全）</li>
 *     <li>{@link #Rng(int)} 的实例内部 {@link RandomGenerator} 为 {@link Random}，
 *     线程安全，但可预测（在加密上不安全）。在竞态条件下会造成争用{@code CAS}</li>
 *     <li>{@link #Rng(RandomGenerator)} 的实例内部 {@link RandomGenerator} 为给定的实例，
 *     线程安全性及密码学上的安全性由内部 {@link RandomGenerator} 实例确定</li>
 * </ul>
 * 一般来说，在相同的 {@code seed} 和 {@link RandomGenerator} 实例类型下，
 * 不同的 {@link Rng} 实例在使用相同的顺序第 {@code n} 次生成的随机数一定是相同的<br>
 *
 * @author baifangkual
 * @see #rollFixLenLarge(int)
 * @since 2024/12/11 v0.0.3
 */
@Default.prov(method = "defaultSingle")
public final class Rng implements RandomGenerator {

    // todo refactor this class not default single instance...
    //   and doc review
    private static class SingleHolder {
        // fixme ThreadLocalRandom.current() 这样可能会造成其在线程间传递... 不能持有该引用
        private static final Rng INSTANCE = new Rng(ThreadLocalRandom.current());
    }

    /**
     * 获取默认单例
     *
     * @return 默认单例
     */
    public static Rng defaultSingle() {
        return SingleHolder.INSTANCE;
    }

    final RandomGenerator refRng; // 委托

    /**
     * 给定种子，创建实例，种子相同的两个实例以相同的顺序第 {@code n} 次生成的随机数一定是相同的
     *
     * @param seed 种子
     */
    public Rng(int seed) {
        refRng = new Random(seed);
    }

    /**
     * 给定伪随机数生成器实例，创建实例，种子相同的两个相同类型实例以相同的顺序第 {@code n} 次生成的随机数一定是相同的
     *
     * @param rng 伪随机数生成器
     */
    public Rng(RandomGenerator rng) {
        this.refRng = rng;
    }

    /**
     * 返回一个伪随机选择的 大数 值<br>
     * <pre>
     *     {@code
     *     int largeNumLength = 100;
     *     String largeNum = Rng.rollFixLenLarge(largeNumLength);
     *     Assert.isTrue(largeNum.length() == largeNumLength);
     *     }
     * </pre>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     */
    public static String rollFixLenLarge(int length) {
        return defaultSingle().nextFixLenLarge(length);
    }

    /**
     * 给定一个值表示长度(数字字符个数)，返回一个数字字符串<br>
     * 该数字取随机数，该数字的数字字符个数为给定的长度值, 返回的数字字符串一定为正整数，一定不以 0 开头<br>
     * <pre>
     *     {@code
     *     int largeNumLength = 5;
     *     String largeNum = Rng.rollFixLenLarge(largeNumLength);
     *     Assert.isTrue(largeNum.length() == largeNumLength);
     *     }
     * </pre>
     *
     * @param length 数字字符个数
     * @throws IllegalArgumentException 当给定的参数值小于1时
     * @apiNote 该方法返回的数字字符串可能在长度过大时无法用整型表达，即不能转为整型
     */
    public String nextFixLenLarge(int length) {
        Err.realIf(length < 1, IllegalArgumentException::new, "Length must > 0");
        if (length == 1) {
            return Const.BASE62_CHARS_LOOKUP_TABLE.get(nextInt(10)); //只要一个那就直接给，不创建str
        } else {
            StringBuilder n = new StringBuilder();
            int len = n.length();
            while (len != length) {
                if (len < length) { // 小了补
                    n.append(nextLong(1, Long.MAX_VALUE));
                } else { // 多了削
                    n.delete(length, Integer.MAX_VALUE);
                }
                len = n.length();
            }
            return n.toString();
        }
    }

    // 直接使用内部实例对应方法，不需要RandomSupport转 ===========================

    /**
     * 返回一个伪随机选择的 long 值
     */
    @Override
    public long nextLong() {
        return refRng.nextLong();
    }

    /**
     * 返回一个伪随机选择的 long 值
     */
    public static long rollLong() {
        return defaultSingle().nextLong();
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [origin, bound)}
     */
    @Override
    public long nextLong(long origin, long bound) {
        return refRng.nextLong(origin, bound);
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [0, bound)}
     */
    @Override
    public long nextLong(long bound) {
        return refRng.nextLong(bound);
    }

    /**
     * 返回一个伪随机选择的 long 值, 区间为 {@code [0, bound)}
     */
    public static long rollLong(long bound) {
        return defaultSingle().nextLong(bound);
    }

    /**
     * 返回一个伪随机选择的 double 值，区间为 {@code [0, 1)}
     */
    @Override
    public double nextDouble() {
        return refRng.nextDouble();
    }

    /**
     * 返回一个伪随机选择的 boolean 值
     */
    public static boolean rollBoolean() {
        return defaultSingle().nextBoolean();
    }

    /**
     * 返回一个伪随机选择的 double 值，区间为 {@code [0, 1)}
     * <pre>
     *     {@code
     *     double rd = Rng.rollDouble()
     *     Assert.gtOrEq(rd, 0);
     *     Assert.lt(rd, 1);
     *     }
     * </pre>
     */
    public static double rollDouble() {
        return defaultSingle().nextDouble();
    }

    // 直接使用内部实例对应方法，不需要RandomSupport转 ===========================

}
