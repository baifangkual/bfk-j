package io.github.baifangkual.bfk.j.mod.vfs.impl;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.exception.IllegalVFSBuildParamsException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;

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
        // todo 20250515 这里原有 final Cfg ocp = Config.ofConfig(config) 浅拷贝过程，
        //  原该类内的readonly引用是拷贝后的，原设计是为了隔离构造VFS的外部的Cfg和VFS内部的cfg引用，
        //  后续Cfg应实现Cloneable并可用于此，（因为有beforeCfgRefBind过程，遂可能该clone必要！
        final Cfg ocp = cfg;
        beforeReadonlyCfgRefBind(ocp);
        this.readonlyCfg = ocp.toReadonly();
        afterReadonlyCfgRefBind(this.readonlyCfg);
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
     * vfs实例在实际实例化时，可校验或变更外界传递的参数，子类型可在该处变更其，默认行为无动作
     *
     * @param cfg 外界传递的vfs连接参数
     */
    protected void beforeReadonlyCfgRefBind(Cfg cfg) {
    }

    /**
     * 该方法被调用阶段属于{@link #beforeReadonlyCfgRefBind(Cfg)} 之后被调用，用以明确检查给定参数是否正确，
     * 默认行为空实现，到该阶段时，vfs实例内部{@link #readonlyCfg}已经有引用且该配置实例不可变
     *
     * @param readonlyCfg 不可变配置实例
     * @throws IllegalVFSBuildParamsException 当给定参数明确会导致vfs构造失败时，显示抛出该
     */
    protected void afterReadonlyCfgRefBind(Cfg readonlyCfg) throws IllegalVFSBuildParamsException {
    }


    /**
     * 当 vfs 已被设定为已关闭，则调用该方法将抛出异常
     */
    protected void ifCloseThrowVFSRuntimeIOE() {
        if (isClosed()) {
            throw new VFSIOException("VFS: " + this + " is closed.");
        }
    }

}
