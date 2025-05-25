package io.github.baifangkual.bfk.j.mod.core.trait;

import io.github.baifangkual.bfk.j.mod.core.lang.R;

/**
 * <b>工厂</b><br>
 * 实现类应是一个工厂（无状态/状态不可变对象），
 * 其能够接受一个{@link C}，创建一个{@link T}，允许其创建过程因各种原因抛出异常<br>
 * 该接口内 {@link #build(Object)} 方法语义要求从外侧观测，方法明确的创建了一个{@link T} 实例
 *
 * @param <C> 工厂接收实体类型
 * @param <T> 工厂创建实体类型
 * @author baifangkual
 * @since 2025/5/17 v0.0.6
 */
@FunctionalInterface
public interface Factory<C, T> {

    /**
     * 接收一个实体，创建一个实体
     *
     * @param cfg 创建实体所需实体
     * @return 被创建的实体
     * @throws RuntimeException 当创建过程因各种原因中断
     * @see #tryBuild(Object)
     */
    T build(C cfg) throws RuntimeException;

    /**
     * 接收一个实体，创建一个实体
     *
     * @param cfg 创建实体所需实体
     * @return {@link R.Ok}（创建成功）| {@link R.Err}（创建失败）
     */
    default R<T> tryBuild(C cfg) {
        try {
            return R.ofOk(build(cfg));
        } catch (Exception e) {
            return R.ofErr(e);
        }
    }
}
