package io.github.baifangkual.bfk.j.mod.vfs.ftp;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSFactory;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;

/**
 * ftp vfs spi
 *
 * @author baifangkual
 * @since 2024/9/13 v0.0.5
 */
@AutoService(VFSFactory.class)
public class FTPVFSFactory implements VFSFactory {
    @Override
    public VFSType support() {
        return VFSType.ftp;
    }

    @Override
    public VFS create(Cfg cfg) {
        return new FTPVirtualFileSystem(cfg);
    }

}
