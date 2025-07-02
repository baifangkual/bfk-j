package io.github.baifangkual.jlib.vfs;

import io.github.baifangkual.jlib.core.Const;

import java.util.Comparator;

/**
 * VFS相关常量、默认值等，引用在此
 *
 * @author baifangkual
 * @since 2024/9/5 v0.0.5
 */
public class VFSDefaults {

    private VFSDefaults() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * todo 20250624 impl XXXCfgOptions option: auto_close
     *      use jdk.CleanUp or finalize
     *      虽然 jdk 的 CleanUp 行为 和 Refence<T> 的背后实现
     *      比较丑陋，但还是可以提供一个选项用以控制 auto_close
     *      这适用于 VFS 和 DBC
     *      后续闲时可实现该
     */

    /**
     * 缓冲区大小(单位字节），组成 buffer array，因为 minio 的 buffer size 要求（大于5MIB小于5GIB）该缓冲区的设定不宜太小
     * 同样因为硬件限制，该缓冲区不宜太大，若要设定小于5MIB的缓冲区，可以将minio缓冲区单独设定，以防止其流错误，
     * 但这样会使不同vfs的拷贝状态流大小不匹配，
     * 通常来讲，较大的缓冲区会降低系统调用的成本，但会对内存造成压力，遂应当取舍最优解
     */
    public static final int BYTE_BUFFER_SIZE = 1024 * 1024 * 5;
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

    /**
     * 比较VFile函数，文件夹优先
     */
    public static final Comparator<VFile> F_COMP_JUST_DIR_FIRST = Comparator
            .comparingInt(f -> f.isDirectory() ? 0 : 1);
    /**
     * 比较VFile函数，字典序（中文无法
     */
    public static final Comparator<VFile> F_COMP_JUST_NAME_SORT = Comparator.comparing(VFile::name);
    /**
     * 比较VFile函数，优先文件夹，其次字典序
     */
    public static final Comparator<VFile> F_COMP_DIR_FIRST_THEN_NAME_SORT = F_COMP_JUST_DIR_FIRST
            .thenComparing(F_COMP_JUST_NAME_SORT);

}
