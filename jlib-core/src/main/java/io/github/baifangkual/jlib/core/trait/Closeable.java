package io.github.baifangkual.jlib.core.trait;


import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.core.panic.PanicException;

/**
 * <b>能够被关闭</b><br>
 * 表示 有状态对象，该对象有是否已关闭的状态，并且能够明确关闭其，可使用{@link #isClosed()}查看该是否已被关闭<br>
 * 因为该接口继承了{@link AutoCloseable}，遂仍旧可使用语法糖 {@code try-with-resource}<br>
 * 该接口不对实现类的重复调用{@link #close()} 方法做约束<br>
 *
 * @author baifangkual
 * @since 2024/10/10 v0.0.3
 */
public interface Closeable extends AutoCloseable {
    /**
     * 返回是否已关闭
     *
     * @return true 已关闭，反之未关闭
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
     * 恐慌关闭<br>
     * 无需在作用域声明预检异常，关闭过程中发生的预检异常将被包装为{@link PanicException}并抛出
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
     * 偷偷摸摸关闭<br>
     * 无需在作用域声明预检异常，但关闭过程中如果发生异常则还是会抛出相应异常，即使该异常为预检异常
     *
     * @apiNote 该方法因为可以抛出预检异常而不在上下文中显式声明要抛出的预检异常，遂从外部/上层方法签名中无法得知可能抛出的
     * 预检异常，使用该方法要明确该方法可能造成的风险
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
     * 斯大林关闭<br>
     * 无需在作用域声明预检异常，关闭过程中发生的异常将被处决
     *
     * @see #close()
     */
    default void stalinClose() {
        try {
            close();
        } catch (Exception e) {/* pass */}
    }
}
