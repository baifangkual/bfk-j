package io.github.baifangkual.jlib.vfs.ftp;

import com.google.auto.service.AutoService;
import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VFSFactory;
import io.github.baifangkual.jlib.vfs.VFSType;

/**
 * ftp vfs spi
 *
 * @author baifangkual
 * @since 2024/9/13 v0.0.5
 */
@AutoService(VFSFactory.class)
public class FTPVFSFactory implements VFSFactory {
    @Override
    public VFSType type() {
        return VFSType.ftp;
    }

    @Override
    public VFS build(Cfg cfg) {
        return new FTPVirtualFileSystem(cfg);
    }

}
