package io.github.baifangkual.bfk.j.mod.core.function;


import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.model.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * <b>函数式接口</b><br>
 * 相较于{@link BiFunction} 表示可能抛出异常的操作<br>
 * 表示函数，两个入参一个出参
 *
 * @author baifangkual
 * @see BiFunction
 * @since 2024/7/15 v0.0.3
 */
@FunctionalInterface
public interface BiFn<P1, P2, Result> extends BiFunction<P1, P2, R<Result, Exception>>,
        FnMutToSafe<BiFunction<P1, P2, R<Result, Exception>>>,
        FnMutToUnSafe<BiFunction<P1, P2, Result>>,
        Serializable {
    /**
     * 表示两入参一出参且可能发生异常的函数
     *
     * @param p1 参数一
     * @param p2 参数二
     * @return 函数执行结果
     * @throws Exception 可能的异常
     */
    Result unsafeApply(P1 p1, P2 p2) throws Exception;

    /**
     * 执行函数，并将函数执行结果和异常包装至{@link R}容器对象
     *
     * @param p1 参数一
     * @param p2 参数二
     * @return 函数执行结果和可能发生异常的容器对象
     */
    @Override
    default R<Result, Exception> apply(P1 p1, P2 p2) {
        return toSafe().apply(p1, p2);
    }

    /**
     * 将函数转为{@link BiFunction}接口的安全函数，函数执行过程的异常将被包装为{@link R}而不会直接抛出
     *
     * @return 安全函数
     */
    @Override
    default BiFunction<P1, P2, R<Result, Exception>> toSafe() {
        return (p1, p2) -> {
            try {
                return R.ofOk(this.unsafeApply(p1, p2));
            } catch (Exception e) {
                return R.ofErr(e);
            }
        };
    }

    /**
     * 将函数转为{@link BiFunction}接口函数，函数执行过程中抛出的异常将被静默抛出，包括预检异常
     *
     * @return 静默抛出可能的异常的非安全函数
     */
    @Override
    default BiFunction<P1, P2, Result> toSneaky() {
        return (p1, p2) -> {
            try {
                return this.unsafeApply(p1, p2);
            } catch (Exception e) {
                Err.throwReal(e);
            }
            // 不会执行到此，只为编译通过
            throw new UnsupportedOperationException();
        };
    }

    /**
     * 将函数转为{@link BiFunction}接口函数，函数执行过程中抛出的异常将被包装为{@link PanicException}运行时异常
     *
     * @return 以PanicException形式抛出可能的异常的非安全函数
     */
    @Override
    default BiFunction<P1, P2, Result> toUnsafe() {
        return (p1, p2) -> {
            try {
                return this.unsafeApply(p1, p2);
            } catch (Exception e) {
                throw PanicException.wrap(e);
            }
        };
    }

}
