package io.github.baifangkual.jlib.vfs;

/**
 * 虚拟文件系统类型
 *
 * @author baifangkual
 * @since 2024/8/23 v0.0.5
 */
public enum VFSType {
    /**
     * smb share 为根的 虚拟文件系统 依赖 smb-cifs协议，windows文件共享
     */
    smb,
    /**
     * ftp
     */
    ftp,
    /**
     * minio 虚拟文件系统 依赖 minio服务
     */
    minio,
    /**
     * 本地文件系统（可使用cfg配置 use temp root使 vfs.root 为临时目录）
     */
    local,
    ;

}
