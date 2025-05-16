package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;
import io.github.baifangkual.bfk.j.mod.vfs.spi.VFSRegister;

import java.util.function.Function;

/**
 * ftp vfs spi
 *
 * @author baifangkual
 * @since 2024/9/13 v0.0.5
 */
@AutoService(VFSRegister.class)
public class FTPVFSRegister implements VFSRegister {
    @Override
    public VFSType supportType() {
        return VFSType.ftp;
    }

    @Override
    public Function<Cfg, VFS> constructorRef() {
        return FTPVirtualFileSystem::new;
    }
}
