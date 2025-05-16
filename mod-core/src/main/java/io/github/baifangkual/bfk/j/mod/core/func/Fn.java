package io.github.baifangkual.bfk.j.mod.core.func;

import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;

import java.io.Serializable;
import java.util.function.Function;

/**
 * <b>函数式接口</b><br>
 * 相较于{@link Function}，表示可能抛出异常的操作<br>
 * 表示函数,一个入参一个出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code Fn<Path, byte[]> fn = Files::readAllBytes;}
 *
 * @author baifangkual
 * @see Function
 * @since 2024/7/15 v0.0.3
 */
@FunctionalInterface
public interface Fn<P, Result> extends Function<P, R<Result, Exception>>,
        FnMutToSafe<Function<P, R<Result, Exception>>>,
        FnMutToUnSafe<Function<P, Result>>,
        Serializable {
    /**
     * 表示一个入参一个出参的函数，函数执行过程中允许抛出异常，包括运行时异常和预检异常
     *
     * @param p 入参
     * @return 出参
     * @throws Exception 函数执行过程中可能抛出的异常
     */
    Result unsafeApply(P p) throws Exception;

    /**
     * 执行函数，函数执行结果将被包装为{@link R}容器对象，函数执行过程抛出的异常也将被引用在{@link R}容器对象中
     *
     * @param p 入参
     * @return {@link R}容器对象，对象中包含函数执行结果（正常执行时）或异常对象（发生异常时）
     */
    @Override
    default R<Result, Exception> apply(P p) {
        return toSafe().apply(p);
    }

    /**
     * 将函数转为{@link Function}类型的安全函数，函数执行过程中发生的异常将被包装在{@link R}容器对象中,
     * 函数的返回值为{@link R}容器对象，对象中包含函数执行结果（正常执行时）或异常对象（发生异常时）
     *
     * @return {@link Function}
     */
    @Override
    default Function<P, R<Result, Exception>> toSafe() {
        return p -> {
            try {
                return R.ofOk(this.unsafeApply(p));
            } catch (Exception e) {
                return R.ofErr(e);
            }
        };
    }

    /**
     * 将函数转为{@link Function}类型的非安全函数，函数执行过程中发生的异常将被包装为{@link PanicException}并抛出
     *
     * @return {@link Function}
     */
    @Override
    default Function<P, Result> toUnsafe() {
        return (p) -> {
            try {
                return this.unsafeApply(p);
            } catch (Exception e) {
                throw PanicException.wrap(e);
            }
        };
    }

    /**
     * 将函数转为{@link Function}类型的非安全函数，函数执行过程中发生的异常将直接抛出，包括运行时异常或预检异常
     *
     * @return {@link Function}
     */
    @Override
    default Function<P, Result> toSneaky() {
        return (p) -> {
            try {
                return this.unsafeApply(p);
            } catch (Exception e) {
                Err.throwReal(e);
            }
            // 不会执行到此，只为编译通过
            throw new UnsupportedOperationException();
        };
    }


}
