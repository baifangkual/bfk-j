package io.github.baifangkual.bfk.j.mod.core.func;

import io.github.baifangkual.bfk.j.mod.core.lang.Nil;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <b>函数式接口 (Function Access)</b><br>
 * 表示函数: {@code (p) -> () | Err}<br>
 * 相较于{@link Consumer} 表示可能抛出异常的操作<br>
 *
 * @param <P> 入参类型
 * @author baifangkual
 * @apiNote call {@link #toSafe()} mut to {@code Function<P, R<Nil>>}<br>
 * @implNote 无法统一该类型与 {@link Consumer} 返回值语义，遂该类型不扩展 {@code Consumer}
 * @since 2025/5/3 v0.0.4
 */
@FunctionalInterface
public interface FnAcc<P> extends
        FnMutToUnSafe<Consumer<P>>,
        FnMutToSafe<Function<P, R<Nil>>>,
        Serializable {

    /**
     * 表示一入参无出参且可能发生异常的函数
     *
     * @param p 入参一
     * @throws Exception 函数执行过程中可能发生的异常
     */
    void unsafeAcc(P p) throws Exception;

    /**
     * 将函数转为{@link Function}类型的安全函数，函数执行过程中发生的异常将被包装在{@link R}容器对象中,
     * 函数的返回值为{@link R}容器对象，对象中包含函数执行结果 {@code Nil}（正常执行时）或异常对象（发生异常时）
     *
     * @return {@link Function}
     */
    @Override
    default Function<P, R<Nil>> toSafe() {
        return (P p) -> {
            try {
                this.unsafeAcc(p);
                return R.ofNil();
            } catch (Exception e) {
                return R.ofErr(e);
            }
        };
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
                this.unsafeAcc(p);
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
                this.unsafeAcc(p);
            } catch (Exception e) {
                Err.throwReal(e);
            }
        };
    }
}
