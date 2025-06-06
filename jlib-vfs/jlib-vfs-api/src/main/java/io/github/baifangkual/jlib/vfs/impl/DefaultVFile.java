package io.github.baifangkual.jlib.vfs.impl;


import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VFile;
import io.github.baifangkual.jlib.vfs.VFileType;
import io.github.baifangkual.jlib.vfs.VPath;

import java.util.Objects;

/**
 * impl {@link VFile}<br>
 *
 * @author baifangkual
 * @since 2024/8/29 v0.0.5
 */
public class DefaultVFile implements VFile {

    private final VFS vfs;
    private final VPath path;
    private final VFileType type;
    private final long sizeOfBytes;

    public DefaultVFile(VFS vfs, VPath path, VFileType type, long sizeOfBytes) {
        this.vfs = Objects.requireNonNull(vfs, "vfs is null");
        this.path = Objects.requireNonNull(path, "path is null");
        this.type = Objects.requireNonNull(type, "type is null");
        this.sizeOfBytes = sizeOfBytes;
    }

    @Override
    public VPath toPath() {
        return path;
    }

    @Override
    public long sizeOfBytes() {
        return sizeOfBytes;
    }

    @Override
    public VFileType type() {
        return type;
    }

    @Override
    public VFS vfs() {
        return vfs;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultVFile vFile)) return false;
        return Objects.equals(vfs, vFile.vfs)
               && Objects.equals(path, vFile.path) && type == vFile.type
               && sizeOfBytes == vFile.sizeOfBytes;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(vfs);
        result = 31 * result + Objects.hashCode(path);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(sizeOfBytes);
        return result;
    }

    @Override
    public String toString() {
        return "VFile(" +
               "vfs=" + vfs +
               ", path=" + path +
               ", type=" + type +
               ", sizeOfBytes=" + sizeOfBytes +
               ')';
    }
}
