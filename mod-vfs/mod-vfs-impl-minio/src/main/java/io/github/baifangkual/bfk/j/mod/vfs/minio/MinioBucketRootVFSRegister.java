package io.github.baifangkual.bfk.j.mod.vfs.minio;

import com.google.auto.service.AutoService;
import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VFSType;
import io.github.baifangkual.bfk.j.mod.vfs.spi.VFSRegister;

import java.util.function.Function;

/**
 * minio vfs spi
 *
 * @author baifangkual
 * @since 2024/9/6
 */
@AutoService(VFSRegister.class)
public class MinioBucketRootVFSRegister implements VFSRegister {
    @Override
    public VFSType supportType() {
        return VFSType.minio_bucket;
    }

    @Override
    public Function<Cfg, VFS> constructorRef() {
        return MinioBucketRootVirtualFileSystem::new;
    }
}
