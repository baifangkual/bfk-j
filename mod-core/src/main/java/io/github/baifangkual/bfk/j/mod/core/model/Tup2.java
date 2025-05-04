package io.github.baifangkual.bfk.j.mod.core.model;

import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.util.Objects;
import java.util.function.Function;

/**
 * 二元组，不可变对象, L R 不可为null
 * impl EQ & HASH & TOStr
 * <pre>
 *     {@code
 *     Tup2<Int, Long> t = Tup2.of(1, 2L);
 *     Tup2<String, Long> mt = t.mapL(String::valueOf);
 *     String l = mt.l();
 *     Long r = mt.r();
 *     }
 * </pre>
 *
 * @author baifangkual
 * @since 2024/6/24
 */
public final class Tup2<L, R> {

    private final L l;
    private final R r;

    private Tup2(L l, R r) {
        Err.realIf(l == null, NullPointerException::new, "Tup2.l is null");
        Err.realIf(r == null, NullPointerException::new, "Tup2.r is null");
        this.l = l;
        this.r = r;
    }


    public static <L, R> Tup2<L, R> of(L l, R r) {
        return new Tup2<>(l, r);
    }

    public L l() {
        return l;
    }

    public R r() {
        return r;
    }

    public <NL> Tup2<NL, R> mapL(Function<? super L, ? extends NL> fn) {
        return map(fn, Function.identity());
    }

    public <NR> Tup2<L, NR> mapR(Function<? super R, ? extends NR> fn) {
        return map(Function.identity(), fn);
    }

    public <NL, NR> Tup2<NL, NR> map(Function<? super L, ? extends NL> lMapFn,
                                     Function<? super R, ? extends NR> rMapFn) {
        return Tup2.of(Objects.requireNonNull(lMapFn).apply(l()), Objects.requireNonNull(rMapFn).apply(r()));
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
}
