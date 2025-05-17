package io.github.baifangkual.bfk.j.mod.core.mark;

import io.github.baifangkual.bfk.j.mod.core.lang.R;

/**
 * <b>工厂标记接口</b><br>
 * 实现类应是一个工厂（无状态/状态不可变对象），其应当能够接受一个{@link C}，创建一个{@link T}，允许其创建过程因各种原因抛出异常
 *
 * @param <C> 产品配置
 * @param <T> 产品类型
 * @author baifangkual
 * @since 2025/5/17 v0.0.6
 */
@FunctionalInterface
public interface Factory<C, T> {

    /**
     * 给定一个配置，创建一个产品
     *
     * @param cfg 创建产品所需的配置
     * @return 产品实例
     * @throws Exception 当创建过程因各种原因中断
     * @see #tryCreate(Object)
     */
    T create(C cfg) throws Exception;

    /**
     * 给定一个配置，创建一个产品
     *
     * @param cfg 创建产品所需的配置
     * @return {@link R.Ok}载荷产品（创建成功）| {@link R.Err}载荷创建过程抛出的异常（创建失败）
     */
    default R<T, Exception> tryCreate(C cfg) {
        try {
            return R.ofOk(create(cfg));
        } catch (Exception e) {
            return R.ofErr(e);
        }
    }
}
