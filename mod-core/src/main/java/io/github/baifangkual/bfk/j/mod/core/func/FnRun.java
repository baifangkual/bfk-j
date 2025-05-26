package io.github.baifangkual.bfk.j.mod.core.func;


import io.github.baifangkual.bfk.j.mod.core.lang.Nil;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * <b>函数式接口 (Function Runnable)</b><br>
 * 表示函数: {@code () -> () | Err}<br>
 * 相较于{@link Runnable} 表示可能抛出异常的操作<br>
 * 表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * 例如 {@code FnRun fn = () -> conn.close();}
 *
 * @author baifangkual
 * @apiNote call {@link #toSafe()} mut to {@code Supplier<R<Nil>>}<br>
 * @implNote 无法统一该类型与 {@link Runnable} 返回值语义，遂该类型不扩展 {@code Runnable}
 * @since 2024/7/15 v0.0.4
 */
@FunctionalInterface
public interface FnRun extends
        FnMutToSafe<Supplier<R<Nil>>>,
        FnMutToUnSafe<Runnable>,
        Serializable {
    /**
     * 表示无入参无出参的函数，执行时可能抛出异常
     *
     * @throws Exception 函数执行过程中可能抛出异常
     */
    void unsafeRun() throws Exception;


    @Override
    default Supplier<R<Nil>> toSafe() {
        return () -> {
            try {
                this.unsafeRun();
                return R.ofNil();
            } catch (Exception e) {
                return new R.Err<>(e);
            }
        };
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
