package io.github.baifangkual.jlib.vfs.smb;

import com.google.auto.service.AutoService;
import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VFSFactory;
import io.github.baifangkual.jlib.vfs.VFSType;

/**
 * vfs smb spi
 *
 * @author baifangkual
 * @since 2024/8/30
 */
@AutoService(VFSFactory.class)
public class SMBVFSFactory implements VFSFactory {
    @Override
    public VFSType type() {
        return VFSType.smb;
    }


    @Override
    public VFS build(Cfg cfg) {
        return new SMBShareRootVirtualFileSystem(cfg);
    }
}
