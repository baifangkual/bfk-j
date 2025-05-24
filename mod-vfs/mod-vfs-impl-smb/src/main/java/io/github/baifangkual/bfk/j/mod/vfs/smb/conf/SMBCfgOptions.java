package io.github.baifangkual.bfk.j.mod.vfs.smb.conf;

import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaultConst;
import io.github.baifangkual.bfk.j.mod.vfs.VFSFactory;

/**
 * smb vfs 连接配置<br>
 * 可通过{@link VFSFactory}创建{@link io.github.baifangkual.bfk.j.mod.vfs.VFS}
 *
 * @author baifangkual
 * @since 2024/8/28 v0.0.5
 */
public class SMBCfgOptions {
    private SMBCfgOptions() {
        throw new IllegalAccessError("Utility class");
    }


    public static final Cfg.Option<String> host = Cfg.Option.of("vfs.smb.host")
            .stringType()
            .description("smb服务器所在域名或ipv4/v6")
            .build();

    public static final Cfg.Option<Integer> port = Cfg.Option.of("vfs.smb.port")
            .intType()
            .defaultValue(445)
            .description("smb服务器端口，默认协议端口445")
            .build();

    public static final Cfg.Option<String> share = Cfg.Option.of("vfs.smb.share.path")
            .stringType()
            .description("smb服务器暴露的share目录，在以share为根的虚拟文件系统中，该参数必需")
            .notFoundValueMsg("未设置smb服务的共享目录")
            .build();

    public static final Cfg.Option<String> user = Cfg.Option.of("vfs.smb.login.user")
            .stringType()
            .description("smb服务器认证-用户名")
            .build();
    public static final Cfg.Option<String> passwd = Cfg.Option.of("vfs.smb.login.passwd")
            .stringType()
            .description("smb服务器认证-密码")
            .build();

    public static final Cfg.Option<String> domain = Cfg.Option.of("vfs.smb.login.domain")
            .stringType()
            .description("smb服务器域")
            .build();
    public static final Cfg.Option<Integer> bufSize = Cfg.Option.of("vfs.smb.buf.size")
            .intType()
            .defaultValue(VFSDefaultConst.BYTE_BUFFER_SIZE)
            .description("smb缓冲区大小")
            .build();

    public static final Cfg.Option<Boolean> acWithAnonymous = Cfg.Option.of("vfs.smb.login.any")
            .booleanType()
            .defaultValue(false)
            .description("smb服务器认证-anonymous")
            .build();
    public static final Cfg.Option<Boolean> acWithGuest = Cfg.Option.of("vfs.smb.login.guest")
            .booleanType()
            .defaultValue(false)
            .description("smb服务器认证-guest")
            .build();

    public static final Cfg.Option<Boolean> withNegotiatedBufSize = Cfg.Option.of("vfs.smb.client.negotiated.buf.size")
            .booleanType()
            .defaultValue(true)
            .description("缓冲区大小是否使用协商的协议支持的最大大小")
            .build();

    public static final Cfg.Option<Boolean> withVersion3 = Cfg.Option.of("vfs.smb.client.version.v3")
            .booleanType()
            .defaultValue(false)
            .description("要求该连接的smb服务器支持v3版本协议，否则抛出异常")
            .build();
}
