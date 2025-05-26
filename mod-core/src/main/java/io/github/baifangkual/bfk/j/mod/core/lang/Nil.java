package io.github.baifangkual.bfk.j.mod.core.lang;

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
    private static final Nil INSTANCE = new Nil();

    private Nil() {/* nil*/}

    /**
     * 返回 {@link Nil} 类型实例
     *
     * @return nil
     */
    public static Nil self() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "nil";
    }
}
