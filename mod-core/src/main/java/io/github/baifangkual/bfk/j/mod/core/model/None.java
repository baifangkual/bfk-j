package io.github.baifangkual.bfk.j.mod.core.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * <b>null</b><br>
 * 明确表达 “null”, 仅可通过方法{@link #ref()} 获取该类型的唯一实例
 *
 * @author baifangkual
 * @since 2024/11/18 v0.0.3
 * @deprecated 尚无该类型使用环境，且无法纳入java.Void体系，后续或考虑删除或优化该
 */
@Deprecated
public final class None implements Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private static final None INSTANCE = new None();

    /**
     * 构造方法私有，不允许构建新实例
     */
    private None() {
    }

    /**
     * 返回{@link None}类型实例
     *
     * @return none
     */
    public static None ref() {
        return INSTANCE;
    }
}
