package io.github.baifangkual.bfk.j.mod.vfs;

/**
 * 目录文件实体类型
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public enum VFileType {
    /**
     * 表示普通文件
     */
    file,
    /**
     * 表示文件夹
     */
    directory,
    /**
     * 表示符号链接、软连接等,
     * 因为增加该类型会增加复杂性，遂打算后续废弃该类型，若有该相关需求，后续可开发
     */
    @Deprecated
    link,
    ;


}
