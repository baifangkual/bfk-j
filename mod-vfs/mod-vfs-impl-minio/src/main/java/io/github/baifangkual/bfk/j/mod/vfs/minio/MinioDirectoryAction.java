package io.github.baifangkual.bfk.j.mod.vfs.minio;


import io.github.baifangkual.bfk.j.mod.vfs.VPath;
import io.github.baifangkual.bfk.j.mod.vfs.exception.VFSIOException;

import java.util.List;

/**
 * 描述minio的文件夹行为
 * <p>该类型实现要求：</p>
 * <p>所有实现中，不应进行{@link VPath}是否为文件夹的检查，默认使用方在操作上层已经进行了检查，并且确定给定的VPath一定为文件夹</p>
 *
 * @author baifangkual
 * @since 2024/9/3 v0.0.5
 */
public interface MinioDirectoryAction {
    /**
     * 查看文件夹是否存在，文件存在与否不在该处查询，应该在该的上层查询
     */
    boolean directoryExists(VPath path) throws VFSIOException;

    /**
     * 展开文件夹中元素，文件和文件夹，判断给定path是否为文件应在上层判断拦截
     */
    List<VPath> lsDir(VPath path, String... excludeNames) throws VFSIOException;

    /**
     * 创建文件夹，判断同目录层级下文件是否已存在应在上层，
     * 额外：在 <a href="https://min.io/docs/minio/linux/developers/java/API.html#putObject">minio.putObject</a> 有说明，
     * 可通过 new ByteArrayInputStream(new byte[] {}), 0, -1) 创建 “文件夹” 但该文件夹可能是行为异常的（其当中会有一个同名文件夹），
     * 遂未使用 官方文档中 说明的 创建 “文件夹“ 的形式
     */
    void mkdir(VPath path) throws VFSIOException;

    /**
     * 删除文件夹，判断元素是否为文件夹应在上层
     */
    void rmDir(VPath path) throws VFSIOException;

}
