package io.github.baifangkual.bfk.j.mod.vfs;

/**
 * 支持的虚拟文件系统类型
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public enum VFSType {
    /**
     * smb share 为根的 虚拟文件系统 依赖 smb-cifs协议，windows文件共享
     */
    smb_share,
    /**
     * ftp
     */
    ftp,
    /**
     * minio 为根的 虚拟文件系统，依赖 minio服务
     */
    minio,
    /**
     * minio bucket 为根的 虚拟文件系统 依赖 minio服务
     */
    minio_bucket,
    /**
     * 本地文件系统
     */
    local,
    /**
     * 本地文件系统临时目录
     */
    local_temp

}
