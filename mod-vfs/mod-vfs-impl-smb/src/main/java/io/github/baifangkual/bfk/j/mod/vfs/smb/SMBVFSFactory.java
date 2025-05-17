package io.github.baifangkual.bfk.j.mod.vfs.smb;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSFactory;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;

/**
 * vfs smb spi
 *
 * @author baifangkual
 * @since 2024/8/30
 */
@AutoService(VFSFactory.class)
public class SMBVFSFactory implements VFSFactory {
    @Override
    public VFSType support() {
        return VFSType.smb;
    }


    @Override
    public VFS create(Cfg cfg) {
        return new SMBShareRootVirtualFileSystem(cfg);
    }
}
