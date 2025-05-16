package io.github.baifangkual.bfk.j.mod.core.func;


import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.io.Serializable;

/**
 * <b>函数式接口</b><br>
 * 相较于{@link Runnable}，表示可能抛出异常的操作<br>
 * 表示函数，构成闭包，无入参出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code FnRun fn = () -> AtomicLong.getAndIncrement();}
 *
 * @author baifangkual
 * @see Runnable
 * @since 2024/7/15 v0.0.4
 */
@FunctionalInterface
public interface FnRun extends Runnable,
        FnMutToUnSafe<Runnable>,
        Serializable {
    /**
     * 表示无入参无出参的函数，执行时可能抛出异常
     *
     * @throws Exception 函数执行过程中可能抛出异常
     */
    void unsafeRun() throws Exception;

    /**
     * 执行函数，函数执行过程中的异常将被包装为{@link PanicException}并抛出，包括运行时和预检异常
     */
    @Override
    default void run() {
        toUnsafe().run();
    }

    /**
     * 将函数转为{@link Runnable}类型的非安全函数，执行过程中发生的异常将直接抛出，包括运行时和预检异常
     *
     * @return {@link Runnable}
     */
    @Override
    default Runnable toSneaky() {
        return () -> {
            try {
                this.unsafeRun();
            } catch (Exception e) {
                Err.throwReal(e);
            }
        };
    }

    /**
     * 将函数转为{@link Runnable}类型的非安全函数，执行过程中发生的异常将被包装为{@link PanicException}并直接抛出，包括运行时和预检异常
     *
     * @return {@link Runnable}
     */
    @Override
    default Runnable toUnsafe() {
        return () -> {
            try {
                this.unsafeRun();
            } catch (Exception e) {
                Err.throwPanic(e);
            }
        };
    }

    // static fn --------------------------------------------------------------------

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，直接抛出相应异常，即使该异常为预检异常<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         FnRun.runOrThrowReal(() -> Files.writeString(Path.of("/xxx"), "xxx"));
     *     }
     * </pre>
     *
     * @param fnRun 可执行语句,可能异常，异常时直接抛出，对预检和运行时异常皆可
     */
    static void runOrThrowReal(FnRun fnRun) {
        fnRun.toSneaky().run();
    }

    /**
     * 执行给定的可能抛出异常的函数，当发生异常时，包装相应异常为{@link PanicException}并抛出<br>
     * <pre>
     *     {@code
     *        try {
     *             Files.writeString(Path.of("/xxx"), "xxx");
     *         } catch (IOException e) {
     *             throw e;
     *         }
     *         //上述语句同下，区别是不会因为 e 为预检异常而需要向当前方法作用域内显式声明throws
     *         FnRun.runOrThrowPanic(() -> Files.writeString(Path.of("/xxx"), "xxx"));
     *     }
     * </pre>
     *
     * @param fnRun 可执行语句,可能异常，预检和运行时异常皆可
     */
    static void runOrThrowPanic(FnRun fnRun) {
        fnRun.toUnsafe().run();
    }

    // static fn --------------------------------------------------------------------


}
