package io.github.baifangkual.bfk.j.mod.vfs.impl;


import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import io.github.baifangkual.bfk.j.mod.vfs.VFS;
import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.exception.IllegalVPathException;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static io.github.baifangkual.bfk.j.mod.vfs.immutable.VFSDefaultConst.PATH_SEPARATOR;

/**
 * @author baifangkual
 * @since 2024/8/28 v0.0.5
 */
public class DefaultSliceAbsolutePath implements VPath {

    private final static String[] EMP = new String[0];

    private final VFS vfs;
    private final String[] pathSlice;

    public DefaultSliceAbsolutePath(VFS vfs, String absolutePath) {
        this(vfs, slicePath(absolutePath));
    }

    private DefaultSliceAbsolutePath(VFS vfs, String[] pathSlice) {
        // todo nonNull check 测试正使用 null 先不填充 null检查
        // todo 20250515 对该进行doc说明补全
        this.vfs = vfs;
        this.pathSlice = pathSlice;
    }

    public static String[] slicePath(String path) {
        Err.realIf(path == null || path.isBlank(), IllegalVPathException::new, "非法的无效路径: \"{}\"", path);
        if (PATH_SEPARATOR.equals(path)) {
            return EMP;
        }
        if (path.startsWith(PATH_SEPARATOR)) {
            path = path.substring(PATH_SEPARATOR.length());
        }
        if (path.endsWith(PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - PATH_SEPARATOR.length());
        }
        return path.split(PATH_SEPARATOR);
    }

    public static String[] slice(String[] tp, int begin, int end) {
        int len = tp.length;
        if (end < 0) {
            end = len + end;
        }
        return Arrays.copyOfRange(tp, begin, end);
    }


    @Override
    public VPath back() {
        if (pathSlice.length == 0) { // 表示当前已经为根目录了，因该类型的不可变性，遂可返回当前而不创建新
            return this;
        }
        return new DefaultSliceAbsolutePath(this.vfs, slice(this.pathSlice, 0, -1));
    }

    @Override
    public VPath back(int numberOfBack) {
        Err.realIf(numberOfBack < 0, IllegalVPathException::new, "numberOfBack不得小于0");
        if (pathSlice.length == 0 || numberOfBack == 0) {
            return this;
        }
        if (numberOfBack > pathSlice.length) {
            return new DefaultSliceAbsolutePath(this.vfs, EMP);
        }
        return new DefaultSliceAbsolutePath(this.vfs, slice(this.pathSlice, 0, -numberOfBack));
    }

    @Override
    public VPath join(String relativePath) {
        Err.realIf(relativePath == null || relativePath.isBlank(), IllegalVPathException::new, "非法的无效路径: \"{}\"", relativePath);
        if (!relativePath.contains(PATH_SEPARATOR)) {
            String[] range = new String[this.pathSlice.length + 1];
            System.arraycopy(this.pathSlice, 0, range, 0, this.pathSlice.length);
            range[this.pathSlice.length] = relativePath;
            return new DefaultSliceAbsolutePath(this.vfs, range);
        } else {
            String[] slice = slicePath(relativePath);
            String[] range = new String[this.pathSlice.length + slice.length];
            System.arraycopy(this.pathSlice, 0, range, 0, this.pathSlice.length);
            System.arraycopy(slice, 0, range, this.pathSlice.length, slice.length);
            return new DefaultSliceAbsolutePath(this.vfs, range);
        }
    }

    @Override
    public VFS getVFileSystem() {
        return vfs;
    }

    @Override
    public boolean isRoot() {
        return pathSlice.length == 0;
    }

    @Override
    public String toString() {
        return simplePath();
    }

    @Override
    public String simplePath() {
        if (pathSlice.length == 0) {
            return PATH_SEPARATOR;
        } else {
            StringBuilder display = new StringBuilder();
            for (String slice : pathSlice) {
                display.append(PATH_SEPARATOR).append(slice);
            }
            return display.toString();
        }
    }

    @Override
    public String realPath() {
        final String sp = simplePath();
        boolean rsp = sp.equals(PATH_SEPARATOR);
        return vfs.realRootPathString().equals(PATH_SEPARATOR) ? sp : (rsp ? vfs.realRootPathString() : vfs.realRootPathString() + sp);
    }

    @Override
    public int level() {
        return pathSlice.length;
    }

    @Override
    public Optional<String> tryLast() {
        if (isRoot()) {
            return Optional.empty();
        } else {
            return Optional.of(pathSlice[pathSlice.length - 1]);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultSliceAbsolutePath that)) return false;
        return Objects.equals(vfs, that.vfs) && Arrays.equals(pathSlice, that.pathSlice);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(vfs);
        result = 31 * result + Arrays.hashCode(pathSlice);
        return result;
    }

    @Override
    public int compareTo(VPath other) {
        if (this == other) return 0;
        return Integer.compare(this.level(), other.level());
    }
}
