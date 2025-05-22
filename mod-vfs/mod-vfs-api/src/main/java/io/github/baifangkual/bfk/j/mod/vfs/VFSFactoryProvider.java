package io.github.baifangkual.bfk.j.mod.vfs;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.lang.R;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSBuildingFailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 虚拟文件系统工厂提供者<br>
 * 该工厂类通过SPI机制获取可用的{@link VFSFactory},
 * 可用这些虚拟文件系统工厂创建虚拟文件系统实例{@link VFS}
 *
 * @author baifangkual
 * @see #build(VFSType, Cfg)
 * @see #getFactory(VFSType)
 * @see #tryBuild(VFSType, Cfg)
 * @since 2024/8/30 v0.0.5
 */
public class VFSFactoryProvider {

    private static final Logger log = LoggerFactory.getLogger(VFSFactoryProvider.class);
    /**
     * spi 注册的
     */
    private static final Map<VFSType, VFSFactory> REGISTERS;

    /*
      通过 spi 注册可用的vfs
     */
    static {
        REGISTERS = new HashMap<>();
        ServiceLoader.load(VFSFactory.class).forEach(p -> {
            VFSType supportType = p.support();
            if (log.isDebugEnabled()) {
                if (REGISTERS.containsKey(supportType)) {
                    log.debug("VFS [{}] duplicate registered", supportType);
                }
            }
            REGISTERS.put(supportType, p);
        });
        if (log.isDebugEnabled()) {
            for (VFSType reg : REGISTERS.keySet()) {
                log.debug("VFS [{}] SPI registered", reg);
            }
        }
    }

    /**
     * 获取可使用的VFS类型<br>
     * 这些通过spi注册
     *
     * @return 可使用的VFS类型
     */
    public static Set<VFSType> supports() {
        return REGISTERS.keySet();
    }

    /**
     * 创建虚拟文件系统<br>
     * 要求给定明确的类型和所需参数
     *
     * @param type 虚拟文件系统类型
     * @param cfg  构造某虚拟文件系统所需的配置
     * @return 虚拟文件系统
     * @throws NullPointerException     给定的虚拟文件系统类型为空时
     * @throws VFSBuildingFailException 找不到类型指定的虚拟文件系统实现时
     */
    public static VFS build(VFSType type, Cfg cfg) {
        VFSFactory factory = getFactory(type)
                .orElseThrow(() -> new VFSBuildingFailException(STF.f("not found vfs impl for {}", type)));
        return factory.build(cfg);
    }

    public static R<VFS, Exception> tryBuild(VFSType type, Cfg cfg) {
        return R.ofCallable(() -> build(type, cfg));
    }

    public static Optional<VFSFactory> getFactory(VFSType type) {
        Objects.requireNonNull(type, "given VFSType is null");
        return Optional.ofNullable(REGISTERS.get(type));
    }

}
