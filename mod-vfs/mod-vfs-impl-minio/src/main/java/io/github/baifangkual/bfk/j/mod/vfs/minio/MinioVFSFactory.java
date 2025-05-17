package io.github.baifangkual.bfk.j.mod.vfs.minio;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;
import io.github.baifangkual.bfk.j.mod.vfs.VFSFactory;

/**
 * minio vfs spi
 *
 * @author baifangkual
 * @since 2024/9/6
 */
@AutoService(VFSFactory.class)
public class MinioVFSFactory implements VFSFactory {
    @Override
    public VFSType support() {
        return VFSType.minio;
    }


    @Override
    public VFS create(Cfg cfg) {
        /*
        vfsType.minio if cfg not found bucket, vPath.root is all bucket
         */
        return new MinioBucketRootVirtualFileSystem(cfg);
    }
}
