package io.github.baifangkual.bfk.j.mod.core.function;


import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.ToFnUnSafe;

/**
 * @author baifangkual
 * create time 2024/7/15
 * <p>
 * <b>函数式接口</b><br>
 * 相较于{@link Runnable}，表示可能抛出异常的操作<br>
 * 表示函数，构成闭包，无入参出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code Run fn = () -> AtomicLong.getAndIncrement();}
 * @see Runnable
 */
@FunctionalInterface
public interface Run extends Runnable, ToFnUnSafe<Runnable> {
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
        try {
            unsafeRun();
        } catch (Exception e) {
            throw PanicException.wrap(e);
        }
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

}
