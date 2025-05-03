package cn.bfk.j.mod.core.function;

import cn.bfk.j.mod.core.exception.PanicException;
import cn.bfk.j.mod.core.model.R;
import cn.bfk.j.mod.core.panic.Err;
import cn.bfk.j.mod.core.trait.fn.Safe;
import cn.bfk.j.mod.core.trait.fn.UnSafe;

import java.util.function.Function;

/**
 * 相较于{@link Function}，表示可能抛出异常的操作<br>
 * 表示函数,一个入参一个出参，表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 * {@code PanickyFun<Path, byte[]> fn = Files::readAllBytes;}
 *
 * @see Function
 */
@FunctionalInterface
public interface Fn<P, Result> extends Function<P, R<Result, Exception>>,
        Safe<Function<P, R<Result, Exception>>>,
        UnSafe<Function<P, Result>> {

    Result unsafeApply(P p) throws Exception;

    @Override
    default R<Result, Exception> apply(P p) {
        return toSafe().apply(p);
    }

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
