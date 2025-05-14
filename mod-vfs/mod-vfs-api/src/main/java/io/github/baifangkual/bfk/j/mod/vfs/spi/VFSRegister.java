package io.github.baifangkual.bfk.j.mod.vfs.spi;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;

import java.util.function.Function;

/**
 * VFS类型支持注册器<br>
 * 所有下级的{@link VFS}实现模块都应该通过java spi以某种方式实现该接口，
 * 当某种VFS以SPI实现该接口后，则VFS可通过{@link io.github.baifangkual.bfk.j.mod.vfs.VFSFactory}发现
 *
 * @author baifangkual
 * @since 2024/8/30 v0.0.5
 */
public interface VFSRegister {
    /**
     * 表明这种类型的VFS实现支持何种类型，一对一，枚举
     */
    VFSType supportType();

    /**
     * 函数或方法引用，表示构造某种VFS的构造方法，要求该方法引用或函数的入参为{@link Cfg}，函数的返回值为{@link VFS}实现类
     */
    Function<Cfg, VFS> constructorRef();

}
