package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.bfk.j.mod.vfs.spi.VFSRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 虚拟文件系统简单工厂
 *
 * @author baifangkual
 * @since 2024/8/30 v0.0.5
 */

public class VFSFactory {

    private static final Logger log = LoggerFactory.getLogger(VFSFactory.class);

    private static final List<VFSRegister> REGISTERS;

    /*
      通过 spi 注册可用的vfs
     */
    static {
        REGISTERS = new ArrayList<>(VFSType.values().length);
        ServiceLoader.load(VFSRegister.class).forEach(REGISTERS::add);
        if (log.isDebugEnabled()) {
            for (VFSRegister reg : REGISTERS) {
                log.info("VFS [{}] 已通过SPI注册", reg.supportType());
            }
        }
    }

    /**
     * 获取可使用的，受支持的VFS类型列表，这些通过spi机制发现
     */
    public static List<VFSType> supportedTypes() {
        return REGISTERS.stream().map(VFSRegister::supportType).toList();
    }

    /**
     * 构造一个虚拟文件系统以供使用，要求给定明确的类型和所需参数，当要求的{@link VFSType}不存在时，抛出异常
     */
    public static VFS build(VFSType vfsType, Cfg vfsConfig) {
        VFSRegister register = REGISTERS.stream().filter(reg -> reg.supportType() == vfsType)
                .findFirst()
                .orElseThrow(() -> new VFSBuildingFailException(STF.f("not found vfs impl for {}", vfsType)));
        return register.constructorRef().apply(vfsConfig);
    }

}
