package io.github.baifangkual.jlib.vfs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.trait.Factory;
import io.github.baifangkual.jlib.vfs.exception.VFSBuildingFailException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <b>VFS工厂</b>
 * 也同为VFS类型实现提供者（支持注册器）<br>
 * 所有下级的{@link VFS}实现模块都应该通过java spi以某种方式实现该接口，
 * 当某种VFS以SPI实现该接口后，则VFS实例可通过 {@link VFSFactory#build(Cfg)} 创建
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
    VFSType type();

    /**
     * 给定{@link Cfg}表示构造该VFS所需配置参数，创建一个{@link VFS}
     *
     * @param cfg 创建VFS所需配置
     * @return VFS
     * @throws VFSBuildingFailException 当给定的参数缺失、异常、无法构造VFS实例时
     */
    @Override
    VFS build(Cfg cfg) throws VFSBuildingFailException;

    /**
     * 根据类型获取 {@link VFSFactory} 实例
     *
     * @param type 类型
     * @return Present(VFSFactory) | Empty()
     * @throws NullPointerException 给定的虚拟文件系统类型为空时
     */
    static Optional<VFSFactory> of(VFSType type) {
        return VFSFactories.getFactory(type);
    }

    /**
     * 获取已注册可用的文件系统类型
     *
     * @return 可用的文件系统类型
     */
    static Set<VFSType> supports() {
        return VFSFactories.supports();
    }

    static Stream<VFSFactory> stream() {
        return VFSFactories.REGISTERS.values().stream();
    }
}
