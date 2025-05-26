package io.github.baifangkual.bfk.j.mod.core.func;


import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToSafe;
import io.github.baifangkual.bfk.j.mod.core.mark.FnMutToUnSafe;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.core.panic.PanicException;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * <b>函数式接口 (Function)</b><br>
 * 表示函数: {@code (p1, p2) -> (v) | E}<br>
 * 相较于{@link BiFunction}，表示可能抛出异常的操作<br>
 * 表示的函数可能带有预检异常或运行时异常声明，可以引用throwable方法<br>
 *
 * @param <P1> 入参一类型
 * @param <P2> 入参二类型
 * @param <V>  出参类型
 * @author baifangkual
 * @apiNote call {@link #toSafe()} mut to {@code BiFunction<P1, P2, R<V>>}<br>
 * @see BiFunction
 * @since 2024/7/15 v0.0.3
 */
@FunctionalInterface
public interface Fn2<P1, P2, V> extends BiFunction<P1, P2, R<V>>,
        FnMutToSafe<BiFunction<P1, P2, R<V>>>,
        FnMutToUnSafe<BiFunction<P1, P2, V>>,
        Serializable {
    /**
     * 表示两入参一出参且可能发生异常的函数
     *
     * @param p1 参数一
     * @param p2 参数二
     * @return 函数执行结果
     * @throws Exception 可能的异常
     */
    V unsafeApply(P1 p1, P2 p2) throws Exception;

    /**
     * 执行函数，并将函数执行结果和异常包装至{@link R}容器对象
     *
     * @param p1 参数一
     * @param p2 参数二
     * @return 函数执行结果和可能发生异常的容器对象
     */
    @Override
    default R<V> apply(P1 p1, P2 p2) {
        return toSafe().apply(p1, p2);
    }

    /**
     * 将函数转为{@link BiFunction}接口的安全函数，函数执行过程的异常将被包装为{@link R}而不会直接抛出
     *
     * @return 安全函数
     */
    @Override
    default BiFunction<P1, P2, R<V>> toSafe() {
        return (p1, p2) -> {
            try {
                return R.ofOk(this.unsafeApply(p1, p2));
            } catch (Exception e) {
                return R.ofErr(e);
            }
        };
    }

    /**
     * 将函数转为{@link BiFunction}接口函数，函数执行过程中抛出的异常将被静默抛出，包括预检异常
     *
     * @return 静默抛出可能的异常的非安全函数
     */
    @Override
    default BiFunction<P1, P2, V> toSneaky() {
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

    /**
     * 将函数转为{@link BiFunction}接口函数，函数执行过程中抛出的异常将被包装为{@link PanicException}运行时异常
     *
     * @return 以PanicException形式抛出可能的异常的非安全函数
     */
    @Override
    default BiFunction<P1, P2, V> toUnsafe() {
        return (p1, p2) -> {
            try {
                return this.unsafeApply(p1, p2);
            } catch (Exception e) {
                throw PanicException.wrap(e);
            }
        };
    }

}
