package io.github.baifangkual.bfk.j.mod.core.function;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.UnSafe;

import java.util.function.Consumer;

/**
 * @author baifangkual
 * create time 2025/5/3
 */
public interface Csm<P> extends Consumer<P>,
        UnSafe<Consumer<P>> {

    void unsafeAccept(P p) throws Exception;

    @Override
    default void accept(P p) {
        try {
            this.unsafeAccept(p);
        } catch (Exception e) {
            throw PanicException.wrap(e);
        }
    }

    @Override
    default Consumer<P> toUnsafe() {
        return (P p) -> {
            try {
                this.unsafeAccept(p);
            } catch (Exception e) {
                throw PanicException.wrap(e);
            }
        };
    }

    @Override
    default Consumer<P> toSneaky() {
        return (P p) -> {
            try {
                this.unsafeAccept(p);
            } catch (Exception e) {
                Err.throwReal(e);
            }
        };
    }
}
