package cn.bfk.j.mod.core.function;


import cn.bfk.j.mod.core.exception.PanicException;
import cn.bfk.j.mod.core.panic.Err;
import cn.bfk.j.mod.core.trait.fn.UnSafe;

/**
 * 表示函数，构成闭包，无入参出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code PanickyRun fn = () -> AtomicLong.getAndIncrement();}
 *
 * @see Runnable
 */
@FunctionalInterface
public interface Run extends Runnable, UnSafe<Runnable> {

    void unsafeRun() throws Exception;

    @Override
    default void run() {
        try {
            unsafeRun();
        } catch (Exception e) {
            throw PanicException.wrap(e);
        }
    }

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
