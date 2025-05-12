package io.github.baifangkual.bfk.j.mod.core.trait;


import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;

/**
 * <b>可关闭的</b><br>
 * 表示 有状态对象，该对象有是否已关闭的状态，并且能够明确关闭其<br>
 * 因为该接口继承了{@link AutoCloseable}，遂仍旧可使用语法糖 try-with-resource<br>
 * 该接口不对实现类的重复调用{@link #close()} 方法做约束<br>
 *
 * @author baifangkual
 * @since 2024/10/10 v0.0.3
 */
public interface Closeable extends AutoCloseable {
    /**
     * 返回该实例是否已关闭
     *
     * @return true:已关闭，false：未关闭
     */
    boolean isClosed();

    /**
     * 关闭该实例
     *
     * @throws Exception 当关闭过程发生异常时
     */
    @Override
    void close() throws Exception;

    /**
     * close方法的方便包装，调用方无需在作用域声明预检异常，关闭过程中发生的预检异常将被包装为{@link PanicException}并抛出
     *
     * @see #close()
     */
    default void panicClose() {
        try {
            close();
        } catch (Exception e) {
            throw PanicException.wrap(e);
        }
    }

    /**
     * close方法的方便包装，调用方无需在作用域声明预检异常，但关闭过程中如果发生异常则还是会抛出相应异常
     *
     * @see #close()
     */
    default void sneakyClose() {
        try {
            close();
        } catch (Exception e) {
            Err.throwReal(e);
        }
    }

    /**
     * close方法的方便包装，调用方无需在作用域声明预检异常，关闭过程中发生的异常将被屏蔽
     *
     * @see #close()
     */
    default void quietlyClose() {
        try {
            close();
        } catch (Exception e) {/* pass */}
    }
}
