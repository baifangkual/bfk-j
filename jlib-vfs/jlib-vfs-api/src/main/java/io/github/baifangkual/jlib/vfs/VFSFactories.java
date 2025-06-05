package io.github.baifangkual.jlib.vfs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 虚拟文件系统工厂提供者<br>
 * 该工厂类通过SPI机制获取可用的{@link VFSFactory},
 * 可用这些虚拟文件系统工厂创建虚拟文件系统实例{@link VFS}
 *
 * @author baifangkual
 * @see VFSFactory#build(Cfg)
 * @see VFSFactory#tryBuild(Object)
 * @since 2024/8/30 v0.0.5
 */
class VFSFactories {

    private static final Logger log = LoggerFactory.getLogger(VFSFactories.class);
    /**
     * spi 注册的
     */
    static final Map<VFSType, VFSFactory> REGISTERS;

    /*
      通过 spi 注册可用的vfs
     */
    static {
        REGISTERS = new HashMap<>();
        ServiceLoader.load(VFSFactory.class).forEach(p -> {
            VFSType supportType = p.type();
            if (log.isDebugEnabled()) {
                if (REGISTERS.containsKey(supportType)) {
                    log.debug("VFS [{}] duplicate registered", supportType);
                }
            }
            REGISTERS.put(supportType, p);
            if (log.isDebugEnabled()) {
                log.debug("VFS [{}] SPI registered by impl type: {}", supportType, p.getClass());
            }
        });
    }

    /**
     * 获取可使用的VFS类型<br>
     * 这些通过spi注册
     *
     * @return 可使用的VFS类型
     */
    static Set<VFSType> supports() {
        return REGISTERS.keySet();
    }

    static Optional<VFSFactory> getFactory(VFSType type) {
        Objects.requireNonNull(type, "given VFSType is null");
        return Optional.ofNullable(REGISTERS.get(type));
    }

}
