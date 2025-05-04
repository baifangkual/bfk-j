package io.github.baifangkual.bfk.j.mod.core.function;


import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.model.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.ToFnSafe;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.ToFnUnSafe;

import java.util.function.Supplier;

/**
 * @author baifangkual
 * create time 2024/7/15
 * <p>
 * <b>函数式接口</b><br>
 * 相较于{@link Supplier}，表示可能抛出异常的操作<br>
 * 表示函数，生产者函数，无入参一个出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code Sup<File> fn = File::getCanonicalFile;}
 * @see Supplier
 */
@FunctionalInterface
public interface Sup<Result> extends Supplier<R<Result, Exception>>,
        ToFnSafe<Supplier<R<Result, Exception>>>,
        ToFnUnSafe<Supplier<Result>> {
    /**
     * 表示无入参一个出参的函数，函数运行过程中允许抛出运行时或预检异常
     *
     * @return 函数运行结果
     * @throws Exception 函数运行过程中允许抛出运行时或预检异常
     */
    Result unsafeGet() throws Exception;

    /**
     * 执行函数，并返回{@link R}类型的容器对象表示结果，函数执行过程中的异常将在返回结果的容器对象中
     *
     * @return 函数执行结果容器对象
     */
    @Override
    default R<Result, Exception> get() {
        return toSafe().get();
    }

    /**
     * 将函数转为{@link Supplier}类型的安全函数，函数执行过程中发生的异常将存在于{@link R}容器对象中
     *
     * @return {@link Supplier}
     */
    @Override
    default Supplier<R<Result, Exception>> toSafe() {
        return () -> {
            try {
                return R.ofOk(this.unsafeGet());
            } catch (Exception e) {
                return R.ofErr(e);
            }
        };
    }

    /**
     * 将函数转为{@link Supplier}类型的非安全函数，函数执行过程中发生的异常将直接抛出，包括运行时和预检异常
     *
     * @return {@link Supplier}
     */
    @Override
    default Supplier<Result> toSneaky() {
        return () -> {
            try {
                return this.unsafeGet();
            } catch (Exception e) {
                Err.throwReal(e);
            }
            // 不会执行到此，只为编译通过
            throw new UnsupportedOperationException();
        };
    }

    /**
     * 将函数转为{@link Supplier}类型的非安全函数，函数执行过程中发生的异常将被包装为{@link PanicException}并抛出
     *
     * @return {@link Supplier}
     */
    @Override
    default Supplier<Result> toUnsafe() {
        return () -> {
            try {
                return this.unsafeGet();
            } catch (Exception e) {
                throw PanicException.wrap(e);
            }
        };
    }

}
