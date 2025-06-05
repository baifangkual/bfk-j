package io.github.baifangkual.jlib.vfs.minio;


import io.github.baifangkual.jlib.vfs.minio.action.FileFlagDirectoryAction;

/**
 * minio 文件夹在vfs中的行为表现策略，两种行为区别具体可看{@link MinioBucketRootVirtualFileSystem} 说明
 *
 * @author baifangkual
 * @since 2024/9/3 v0.0.5
 */
public enum MinioDirectoryActionStrategy {
    /**
     * 文件标志文件夹行为策略
     *
     * @deprecated see {@link FileFlagDirectoryAction} doc
     */
    @Deprecated
    FILE_FLAG,
    /**
     * minio原生文件夹行为策略
     * <ul>
     *     <li>所有位置都为文件夹，且都已存在</li>
     *     <li>同目录下若存在文件，则文件夹不可访问</li>
     * </ul>
     */
    MINIO_NATIVE,
    /**
     * minio API 中的 文件夹行为定义，见 <a href="https://min.io/docs/minio/linux/developers/java/API.html#putObject">...</a>
     * <p>当其创建文件夹时，实际创建了文件夹并在文件夹中放置了一个文件夹自身名字的实体
     * <p>20240906 经过测试观察，使用该在创建和删除以及表示存在与否都比较好，或考虑优先使用该
     */
    MINIO_API
}
