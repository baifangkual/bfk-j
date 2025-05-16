package io.github.baifangkual.bfk.j.mod.vfs;

import io.github.baifangkual.bfk.j.mod.core.lang.Const;

/**
 * VFS相关常量、默认值等，引用在此
 *
 * @author baifangkual
 * @since 2024/9/5 v0.0.5
 */
public class VFSDefaultConst {

    private VFSDefaultConst() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 缓冲区大小(单位字节），组成 buffer array，因为 minio 的 buffer size 要求（大于5MIB小于5GIB）该缓冲区的设定不宜太小
     * 同样因为硬件限制，该缓冲区不宜太大，若要设定小于5MIB的缓冲区，可以将minio缓冲区单独设定，以防止其流错误，
     * 但这样会使不同vfs的拷贝状态流大小不匹配，
     * 通常来讲，较大的缓冲区会降低系统调用的成本，但会对内存造成压力，遂应当取舍最优解
     */
    public static final int BYTE_BUFFER_SIZE = 1024 * 1024 * 8;
    /**
     * 路径分隔符（在VFS系统中的路径分隔符表示）
     */
    public static final String PATH_SEPARATOR = Const.String.SLASH;
    /**
     * 当前目录引用
     */
    public static final String CURR_PATH = Const.String.DOT;
    /**
     * 父级目录引用
     */
    public static final String PARENT_PATH = Const.String.DOUBLE_DOT;
}
