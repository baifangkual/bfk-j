package io.github.baifangkual.jlib.vfs.impl;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.exception.IllegalVFSBuildParamsException;
import io.github.baifangkual.jlib.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;

import java.util.HashMap;

/**
 * 顶层vfs公共抽象，内有构造该VFS的配置类的只读引用
 *
 * @author baifangkual
 * @since 2024/8/26 v0.0.5
 */
public abstract class AbstractVirtualFileSystem implements VFS {

    private final Cfg readonlyCfg;

    /**
     * 该构造应为所有子类型统一构造的入口，在该构造内，子类型有权校验及变更给定的配置,
     * 可能会影响给定的参数
     *
     * @param cfg 外界传递的vfs连接参数
     * @throws VFSBuildingFailException 当构造过程发生异常时，应由该包装或抛出
     */
    public AbstractVirtualFileSystem(Cfg cfg) throws VFSBuildingFailException {
        if (cfg == null) {
            throw new IllegalVFSBuildParamsException("given VFS cfg is null");
        }
        // 对cfg内map做新的map，使内外cfg无关联，当然，map内部更深的引用还是同一个
        final Cfg ocp = Cfg.ofMap(new HashMap<>(cfg.toReadonlyMap()));
        postCfgCopy(ocp);
        this.readonlyCfg = ocp.toReadonly();
        postReadonlyCfgBind(this.readonlyCfg);
    }

    /**
     * 返回该类型携带的配置实例，该实例不可变，仅能查询配置，该方法仅供子类
     *
     * @return 不可变配置视图
     */
    protected final Cfg readonlyCfg() {
        return readonlyCfg;
    }

    /**
     * vfs实例在实际实例化时，可校验或变更外界传递的参数，子类型可在该处变更其，默认行为无动作<br>
     * 该方法被回调的阶段是在 AbstractVirtualFileSystem 拷贝 Cfg 之后，
     * 即该方法修改的可变 cfg 是独立的（浅拷贝外界给定的Cfg中的map）
     *
     * @param cfg 外界传递的vfs连接参数
     */
    protected void postCfgCopy(Cfg cfg) {
    }

    /**
     * 该方法被调用阶段属于{@link #postCfgCopy(Cfg)} 之后被调用，用以明确检查给定参数是否正确，
     * 默认行为空实现，到该阶段时，vfs实例内部{@link #readonlyCfg}已经有引用且该配置实例不可变
     *
     * @param readonlyCfg 不可变配置实例
     * @throws IllegalVFSBuildParamsException 当给定参数明确会导致vfs构造失败时，显示抛出该
     */
    protected void postReadonlyCfgBind(Cfg readonlyCfg) throws IllegalVFSBuildParamsException {
    }


    /**
     * 当 vfs 已被设定为已关闭，则调用该方法将抛出异常
     */
    protected void ifClosedThrowVFSRtIOE() throws VFSIOException {
        if (isClosed()) {
            throw new VFSIOException("VFS: " + this + " is closed.");
        }
    }

}
