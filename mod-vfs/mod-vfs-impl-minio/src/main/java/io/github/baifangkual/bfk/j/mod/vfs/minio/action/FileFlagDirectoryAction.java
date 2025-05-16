package io.github.baifangkual.bfk.j.mod.vfs.minio.action;

/**
 * 以文件标识存在的目录，文件名固定或根据所在目录名称可变，以该方式实现需要vfs完全管理bucket才最佳，
 * 否则则需要兜底部分行为
 *
 * @author baifangkual
 * @since 2024/9/3 v0.0.5
 * @deprecated 该实现实际上是一种没有办法的办法，遂不应当使用和实现该，且似乎后续minio官方api会更新对文件夹的支持，该类废弃
 */
@Deprecated
public class FileFlagDirectoryAction {
    /**
     * 以该表示当前目录的存在
     */
    private static final String DEFAULT_DIR_FLAG = "...";
    /**
     * 以该，8个字节表示表示当前目录存在的文件的字节
     */
    private static final byte[] FLAG_F_BYTES = new byte[]{0x0f, 0x75, 0x0c, 0x6b, 0x00, 0x0d, 0x69, 0x72};


    /*
    // 已测试其不可使用 . 和 .. 遂使用 ...
    // 或者 使用 默认所有 “文件夹“ 存在或都不存在
    // bucket 下 使用 “” 或 null 皆可 ，头无需带 /，且所有次级查都应以 / 为结尾
    // item中 objectName 为 url编码形式，需解码
    // 不存在的目录不会异常，而是一个空目录
    // https://www.minio.org.cn/docs/minio/linux/administration/concepts.html
    // 没有文件夹概念，仅有 “前缀” 概念以模仿文件夹
    // 原生似乎无法查询文件夹属性
    // 经测试，args中各项，似乎必须要指定bucket，否则构建问题
     */

}
