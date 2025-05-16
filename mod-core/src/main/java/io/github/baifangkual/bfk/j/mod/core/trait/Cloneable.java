package io.github.baifangkual.bfk.j.mod.core.trait;

import io.github.baifangkual.bfk.j.mod.core.lang.R;

/**
 * <b>可克隆的</b><br>
 * 克隆自身
 *
 * @param <T> 自限定，自身类型
 * @author baifangkual
 * @apiNote 这里的“克隆”默认指的是“浅拷贝克隆”，即只复制基本类型字段和对象引用，不复制引用指向的对象本身，
 * 该接口的实现类并不一定要调用{@link Object#clone()}，仅实现该接口描述的克隆语义即可
 * @implNote 因为 java.lang.Cloneable 设计的并不好，遂另有此接口表示标记，并用泛型协变遮蔽了返回值类型
 * @since 2025/5/16 v0.0.5
 */
public interface Cloneable<T extends Cloneable<T>> extends java.lang.Cloneable {

    /**
     * 克隆自身（浅拷贝），返回新实例<br>
     * 调用该方法，返回一个新的当前类型的实例（浅拷贝），
     * 该方法允许在某些状态下（非原子的状态切换过程中）被调用时抛出运行时异常
     *
     * @return 新实例（浅拷贝）
     * @apiNote 因为接口的default方法优先级比超类低，遂实现类应显式实现该方法
     */
    T clone();

    /**
     * 尝试克隆自身（浅拷贝），过程可能失败<br>
     * 在实例的某些状态下调用该方法（非原子的状态切换过程中）将返回{@link R.Err}
     *
     * @return 新实例（浅拷贝）| RuntimeErr
     */
    default R<T, RuntimeException> tryClone() {
        return R.ofSupplier(this::clone);
    }
}
