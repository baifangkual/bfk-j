package cn.bfk.j.mod.core.function;


import cn.bfk.j.mod.core.exception.PanicException;
import cn.bfk.j.mod.core.model.R;
import cn.bfk.j.mod.core.panic.Err;
import cn.bfk.j.mod.core.trait.fn.Safe;
import cn.bfk.j.mod.core.trait.fn.UnSafe;

import java.util.function.BiFunction;

/**
 * @author baifangkual
 * create time 2024/7/15
 * <p>
 * 相较于{@link BiFunction} 表示可能抛出异常的操作<br>
 * @see BiFunction
 */
@FunctionalInterface
public interface BiFn<P1, P2, Result> extends BiFunction<P1, P2, R<Result, Exception>>,
        Safe<BiFunction<P1, P2, R<Result, Exception>>>,
        UnSafe<BiFunction<P1, P2, Result>> {

    Result unsafeApply(P1 p1, P2 p2) throws Exception;

    @Override
    default R<Result, Exception> apply(P1 p1, P2 p2) {
        return toSafe().apply(p1, p2);
    }

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
