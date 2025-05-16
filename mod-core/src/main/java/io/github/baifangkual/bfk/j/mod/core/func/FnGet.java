package io.github.baifangkual.bfk.j.mod.core.func;


import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * <b>函数式接口</b><br>
 * 相较于{@link Supplier}，表示可能抛出异常的操作<br>
 * 表示函数，生产者函数，无入参一个出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code FnGet<File> fn = File::getCanonicalFile;}
 *
 * @author baifangkual
 * @see Supplier
 * @since 2024/7/15 v0.0.4
 */
@FunctionalInterface
public interface FnGet<Result> extends Supplier<R<Result, Exception>>, Callable<Result>,
        FnMutToSafe<Supplier<R<Result, Exception>>>,
        FnMutToUnSafe<Supplier<Result>>,
        Serializable {
    /**
     * 表示无入参一个出参的函数，函数运行过程中允许抛出运行时或预检异常
     *
     * @return 函数运行结果
     * @throws Exception 函数运行过程中允许抛出运行时或预检异常
     * @see #call()
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
     * 对{@link Callable#call()}的默认实现<br>
     * 对该方法调用等效于调用{@link #unsafeGet()}
     *
     * @return 函数运行结果
     * @throws Exception 函数运行过程中允许抛出运行时或预检异常
     * @see #unsafeGet()
     */
    @Override
    default Result call() throws Exception {
        return unsafeGet();
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

    // static fn ---------------------------------------------------------------

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，直接抛出相应异常，即使该异常为预检异常<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = FnGet.getOrThrowReal(() -> Files.readAllBytes(Path.of("/xxx")));
     *     }
     * </pre>
     *
     * @param fnGet 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    static <R> R getOrThrowReal(FnGet<? extends R> fnGet) {
        return fnGet.toSneaky().get();
    }

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，包装相应异常为{@link PanicException}并抛出<br>
     * <pre>
     *     {@code
     *        try {
     *             byte[] bytes = Files.readAllBytes(Path.of("/xxx"));
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         // 上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         byte[] bytes = FnGet.getOrThrowPanic(() -> Files.readAllBytes(Path.of("/xxx")));
     *     }
     * </pre>
     *
     * @param fnGet 可执行语句,可能异常，预检和运行时异常皆可
     */
    static <R> R getOrThrowPanic(FnGet<? extends R> fnGet) {
        return fnGet.toUnsafe().get();
    }
    // static fn ---------------------------------------------------------------

}
