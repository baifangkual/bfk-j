package io.github.baifangkual.bfk.j.mod.vfs.smb;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;
import io.github.baifangkual.bfk.j.mod.vfs.spi.VFSRegister;

import java.util.function.Function;

/**
 * vfs smb spi
 *
 * @author baifangkual
 * @since 2024/8/30
 */
@AutoService(VFSRegister.class)
public class SMBShareRootVFSRegister implements VFSRegister {
    @Override
    public VFSType supportType() {
        return VFSType.smb_share;
    }

    @Override
    public Function<Cfg, VFS> constructorRef() {
        return SMBShareRootVirtualFileSystem::new;
    }
}
