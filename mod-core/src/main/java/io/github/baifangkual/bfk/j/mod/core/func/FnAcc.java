package io.github.baifangkual.bfk.j.mod.core.func;

import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * <b>函数式接口</b><br>
 * 相较于{@link Consumer} 表示可能抛出异常的操作<br>
 * 表示函数，一个入参零个出参
 *
 * @author baifangkual
 * @see Consumer
 * @since 2025/5/3 v0.0.4
 */
@FunctionalInterface
public interface FnAcc<P> extends Consumer<P>,
        FnMutToUnSafe<Consumer<P>>,
        Serializable {
    /**
     * 表示一入参无出参且可能发生异常的函数
     *
     * @param p 入参一
     * @throws Exception 函数执行过程中可能发生的异常
     */
    void unsafeAccept(P p) throws Exception;

    /**
     * 执行函数，过程中抛出的异常将被包装为{@link PanicException}并抛出
     *
     * @param p 入参一
     */
    @Override
    default void accept(P p) {
        toUnsafe().accept(p);
    }

    /**
     * 将函数转为{@link Consumer}，函数执行过程中的异常将被包装为{@link PanicException}并抛出
     *
     * @return {@link Consumer}
     */
    @Override
    default Consumer<P> toUnsafe() {
        return (P p) -> {
            try {
                this.unsafeAccept(p);
            } catch (Exception e) {
                Err.throwPanic(e);
            }
        };
    }

    /**
     * 将函数转为{@link Consumer}，函数执行过程中的异常将被直接抛出，包括运行时异常和预检异常
     *
     * @return {@link Consumer}
     */
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
