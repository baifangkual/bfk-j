package io.github.baifangkual.jlib.core.lang;

import java.io.Serial;
import java.io.Serializable;

/**
 * <b>无值</b><br>
 * 明确表达"无值", 类似 java 中 {@code Void}，kt 中 {@code Unit}，rust 中 {@code ()}
 *
 * @author baifangkual
 * @since 2024/11/18 v0.0.3
 */
public final class Nil implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final Nil nil = new Nil();

    private Nil() {/* nil*/}

    /**
     * 返回 {@link Nil} 类型实例
     *
     * @return nil
     */
    public static Nil nil() {
        return nil;
    }

    @Override
    public String toString() {
        return "nil";
    }
}
