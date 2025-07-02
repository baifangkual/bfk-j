package io.github.baifangkual.jlib.vfs.fs;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VFSType;
import io.github.baifangkual.jlib.vfs.VFile;
import io.github.baifangkual.jlib.vfs.VPath;
import io.github.baifangkual.jlib.vfs.exception.VFSBuildingFailException;
import io.github.baifangkual.jlib.vfs.exception.VFSIOException;
import io.github.baifangkual.jlib.vfs.impl.AbstractVirtualFileSystem;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 本地文件系统 vfs impl
 *
 * @author baifangkual
 * @since 2025/6/10
 */
@Deprecated // 未完成
public class LocalFSVirtualFileSystem extends AbstractVirtualFileSystem implements VFS {

    public LocalFSVirtualFileSystem(Cfg cfg) throws VFSBuildingFailException {
        super(cfg);
    }

    @Override
    public VFSType type() {
        return VFSType.local;
    }

    @Override
    public VPath root() {
        return null;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public List<VPath> lsDir(VPath path) throws VFSIOException {
        return List.of();
    }

    @Override
    public VFile mkdir(VPath path) throws VFSIOException {
        return null;
    }

    @Override
    public void rmFile(VPath path) throws VFSIOException {

    }

    @Override
    public void rmdir(VPath path, boolean recursive) throws VFSIOException {

    }

    @Override
    public Optional<VFile> file(VPath path) throws VFSIOException {
        return Optional.empty();
    }

    @Override
    public InputStream fileInputStream(VFile file) throws VFSIOException {
        return null;
    }

    @Override
    public VFile mkFile(VPath path, InputStream newFileData) throws VFSIOException {
        return null;
    }
}
