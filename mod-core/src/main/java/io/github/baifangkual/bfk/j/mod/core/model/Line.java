package io.github.baifangkual.bfk.j.mod.core.model;

import lombok.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

/**
 * 有向线对象，表示两个实体之间的关系，两个实体一个为begin，一个为end
 *
 * @author baifangkual
 * @since 2024/6/18
 */
public class Line<P> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final P begin;
    private final P end;

    public Line(@NonNull P begin, @NonNull P end) {
        this.begin = begin;
        this.end = end;
    }

    public static <P> Line<P> of(P begin, P end) {
        return new Line<>(begin, end);
    }

    public P begin() {
        return begin;
    }

    public P end() {
        return end;
    }

    public <B> Line<B> map(@NonNull Function<? super P, ? extends B> mapFn) {
        return Line.of(mapFn.apply(begin), mapFn.apply(end));
    }

    public Line<P> reverse() {
        return Line.of(end, begin);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Line<?> line)) return false;
        return Objects.equals(begin, line.begin) && Objects.equals(end, line.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(begin, end);
    }

    @Override
    public String toString() {
        return "Line[ " + begin + " -> " + end + " ]";
    }
}
