package io.github.baifangkual.jlib.vfs.impl;


import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.VPath;
import io.github.baifangkual.jlib.vfs.exception.IllegalVPathException;

import java.util.Arrays;
import java.util.Objects;

import static io.github.baifangkual.jlib.vfs.VFSDefaults.PATH_SEPARATOR;

/**
 * impl {@link VPath}<br>
 * 以字符串数组切片形式存储一级一级目录，该形式好对目录层级Path进行管理，
 * 但若内存中过多，则可能造成十分分散的数组引用内存片段
 *
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
        this.vfs = vfs;
        this.pathSlice = pathSlice;
    }

    private static String[] slicePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalVPathException("path is null or empty");
        }
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

    @SuppressWarnings("SameParameterValue")
    private static String[] slice(String[] tp, int begin, int end) {
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
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalVPathException("path is join null or empty");
        }
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
    public VFS vfs() {
        return vfs;
    }

    @Override
    public boolean isRoot() {
        return pathSlice.length == 0;
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
    public int level() {
        return pathSlice.length;
    }

    @Override
    public String name() {
        if (isRoot()) {
            return PATH_SEPARATOR;
        } else {
            return pathSlice[pathSlice.length - 1];
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

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(VPath other) {
        if (this == other) return 0;
        return Integer.compare(this.level(), other.level());
    }

    @Override
    public String toString() {
        return simplePath();
    }
}
