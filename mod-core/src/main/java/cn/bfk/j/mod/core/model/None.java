package cn.bfk.j.mod.core.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author baifangkual
 * create time 2024/11/18
 * <p>
 * 明确表达 “null”, 仅可通过方法{@link #ref()} 获取该类型的唯一实例
 */
public final class None implements Serializable {

    @Serial
    private static final long serialVersionUID = 0L;

    private static final None INSTANCE = new None();

    /**
     * 构造方法私有，不允许构建新实例
     */
    private None() {
    }

    public static None ref() {
        return INSTANCE;
    }
}
