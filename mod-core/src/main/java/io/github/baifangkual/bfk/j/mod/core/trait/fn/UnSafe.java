package io.github.baifangkual.bfk.j.mod.core.trait.fn;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;

/**
 * @author baifangkual
 * create time 2025/5/3
 * <p>
 * <b>不安全函数标记接口</b>
 * 被标记的函数执行{@link #toUnsafe()}方法后执行时将会将可能的异常包装为{@link PanicException}并抛出<br>
 * 被标记的函数执行{@link #toSneaky()}方法后执行时将会将可能的异常直接抛出
 */
public interface UnSafe<FnUnsafe> {
    /**
     * 调用该方法将函数转为“不安全函数”， 该类型不安全函数执行过程中若发生异常，则异常将包装为运行时异常{@link PanicException}并抛出
     *
     * @return 不安全函数
     */
    FnUnsafe toUnsafe();

    /**
     * 调用该方法将函数转为“不安全函数”， 该类型不安全函数的执行过程中若发生异常，则异常将静默抛出
     *
     * @return 不安全函数
     */
    FnUnsafe toSneaky();
}
