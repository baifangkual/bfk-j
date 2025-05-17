package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.core.mark.Factory;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;

/**
 * VFS类型实现提供者（支持注册器）<br>
 * 所有下级的{@link VFS}实现模块都应该通过java spi以某种方式实现该接口，
 * 当某种VFS以SPI实现该接口后，则VFS可通过{@link VFSFactoryProvider}发现
 *
 * @author baifangkual
 * @since 2024/8/30 v0.0.5
 */
public interface VFSFactory extends Factory<Cfg, VFS> {
    /**
     * 返回该提供者可提供哪种类型的{@link VFS}
     *
     * @return VFS类型
     */
    VFSType support();

    /**
     * 给定{@link Cfg}表示构造该VFS所需配置参数，创建一个{@link VFS}
     *
     * @param cfg 创建VFS所需配置
     * @return VFS
     * @throws VFSBuildingFailException 当给定的参数缺失、异常、无法构造VFS实例时
     */
    @Override
    VFS create(Cfg cfg) throws VFSBuildingFailException;
}
