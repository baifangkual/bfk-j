package io.github.baifangkual.bfk.j.mod.core.function;


import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.model.R;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.Safe;
import io.github.baifangkual.bfk.j.mod.core.trait.fn.UnSafe;

import java.util.function.Supplier;

/**
 * 表示函数，生产者函数，无入参一个出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code PanickySup<File> fn = File::getCanonicalFile;}
 *
 * @see Supplier
 */
@FunctionalInterface
public interface Sup<Result> extends Supplier<R<Result, Exception>>,
        Safe<Supplier<R<Result, Exception>>>,
        UnSafe<Supplier<Result>> {

    Result unsafeGet() throws Exception;

    @Override
    default R<Result, Exception> get() {
        return toSafe().get();
    }

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
