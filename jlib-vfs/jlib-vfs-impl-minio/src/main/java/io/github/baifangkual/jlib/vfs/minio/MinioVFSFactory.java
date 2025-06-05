package io.github.baifangkual.jlib.vfs.minio;

import com.google.auto.service.AutoService;
import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VFSType;
import io.github.baifangkual.jlib.vfs.VFSFactory;

/**
 * minio vfs spi
 *
 * @author baifangkual
 * @since 2024/9/6
 */
@AutoService(VFSFactory.class)
public class MinioVFSFactory implements VFSFactory {
    @Override
    public VFSType type() {
        return VFSType.minio;
    }


    @Override
    public VFS build(Cfg cfg) {
        /*
        vfsType.minio if cfg not found bucket, vPath.root is all bucket
         */
        return new MinioBucketRootVirtualFileSystem(cfg);
    }
}
