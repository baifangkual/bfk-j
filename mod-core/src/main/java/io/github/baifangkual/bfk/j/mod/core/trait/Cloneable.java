package io.github.baifangkual.bfk.j.mod.core.trait;

import io.github.baifangkual.bfk.j.mod.core.lang.R;

/**
 * <b>能够浅拷贝自身</b><br>
 *
 * @param <T> 自限定，自身类型
 * @author baifangkual
 * @apiNote 这里的“克隆”默认指的是“浅拷贝克隆”，即只复制基本类型字段和对象引用，不复制引用指向的对象本身，
 * 该接口的实现类并不一定要调用{@link Object#clone()}，仅实现该接口描述的克隆语义即可。<br>
 * 因为java接口的default方法优先级比超类低，遂实现类应显式实现该{@link #clone()}方法
 * @implNote 因为 java.lang.Cloneable 设计的并不好，遂另有此接口表示标记，并用泛型协变遮蔽了返回值类型
 * @implSpec 实现类实现的克隆方法允许在某些状态下（例如非原子的状态切换过程中）被调用时抛出运行时异常
 * @since 2025/5/16 v0.0.5
 */
public interface Cloneable<T extends Cloneable<T>> extends java.lang.Cloneable {

    /**
     * 克隆自身（浅拷贝），返回新实例<br>
     *
     * @return 新实例（浅拷贝）
     */
    T clone();

    /**
     * 尝试克隆自身（浅拷贝），过程可能失败
     *
     * @return 新实例（浅拷贝）| RuntimeErr
     */
    default R<T, RuntimeException> tryClone() {
        return R.ofSupplier(this::clone);
    }
}
