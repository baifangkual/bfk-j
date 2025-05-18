package io.github.baifangkual.bfk.j.mod.core.lang;

import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.Cloneable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * <b>二元组</b><br>
 * 不可变对象, L R 均不允许为null
 * <pre>
 *     {@code
 *     Tup2<Integer, Long> t = Tup2.of(1, 2L);
 *     Tup2<String, Long> mt = t.mapL(String::valueOf);
 *     String l = mt.l();
 *     Long r = mt.r();
 *     }
 * </pre>
 *
 * @author baifangkual
 * @since 2024/6/24 v0.0.3
 */
public final class Tup2<L, R> implements Serializable, Cloneable<Tup2<L, R>> {

    @Serial
    private static final long serialVersionUID = 2L;
    /**
     * left value
     */
    private final L l;
    /**
     * right value
     */
    private final R r;

    private Tup2(L l, R r) {
        Err.realIf(l == null, NullPointerException::new, "Tup2.l is null");
        Err.realIf(r == null, NullPointerException::new, "Tup2.r is null");
        this.l = l;
        this.r = r;
    }

    /**
     * 创建新二元组<br>
     * 不允许空值
     *
     * @param l   左值
     * @param r   右值
     * @param <L> 左值类型
     * @param <R> 右值类型
     * @return 二元组
     * @throws NullPointerException 当给定的两个值中有空值时
     */
    public static <L, R> Tup2<L, R> of(L l, R r) {
        return new Tup2<>(l, r);
    }

    /**
     * 返回左值
     *
     * @return 左值
     */
    public L l() {
        return l;
    }

    /**
     * 返回右值
     *
     * @return 右值
     */
    public R r() {
        return r;
    }

    /**
     * 根据函数转换左值
     *
     * @param fn   左值转换函数
     * @param <NL> 左值新类型
     * @return 新的二元组
     * @throws NullPointerException 当给定的函数为null时
     */
    public <NL> Tup2<NL, R> mapL(Function<? super L, ? extends NL> fn) {
        return map(fn, Function.identity());
    }

    /**
     * 根据函数转换右值
     *
     * @param fn   右值转换函数
     * @param <NR> 右值新类型
     * @return 新的二元组
     * @throws NullPointerException 当给定的函数为null时
     */
    public <NR> Tup2<L, NR> mapR(Function<? super R, ? extends NR> fn) {
        return map(Function.identity(), fn);
    }

    /**
     * 根据函数转换左值和右值
     *
     * @param lMapFn 左值转换函数
     * @param rMapFn 右值转换函数
     * @param <NL>   新左值类型
     * @param <NR>   新右值类型
     * @return 新的二元组
     * @throws NullPointerException 当给定的两个函数至少一个为null时
     */
    public <NL, NR> Tup2<NL, NR> map(Function<? super L, ? extends NL> lMapFn,
                                     Function<? super R, ? extends NR> rMapFn) {
        return Tup2.of(
                Objects.requireNonNull(lMapFn, "lMapFn is null")
                        .apply(l()),
                Objects.requireNonNull(rMapFn, "rMapFn is null")
                        .apply(r())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tup2<?, ?> tup2)) return false;
        return Objects.equals(l, tup2.l) && Objects.equals(r, tup2.r);
    }

    @Override
    public int hashCode() {
        return Objects.hash(l, r);
    }

    @Override
    public String toString() {
        return "Tup2(" + l + ", " + r + ')';
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Tup2<L, R> clone() {
        return Tup2.of(l, r);
    }
}
