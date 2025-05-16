package io.github.baifangkual.bfk.j.mod.vfs.ftp.conf;


import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaultConst;

/**
 * ftp vfs 连接配置
 *
 * @author baifangkual
 * @since 2024/9/9 v0.0.5
 */
public class FTPCfgOptions {

    private FTPCfgOptions() {
        throw new UnsupportedOperationException("Const class");
    }

    public static final Cfg.Option<String> host = Cfg.Option.of("vfs.ftp.host")
            .stringType()
            .description("ftp服务器host")
            .notFoundValueMsg("未设置ftp服务器host")
            .build();

    public static final Cfg.Option<Integer> port = Cfg.Option.of("vfs.ftp.port")
            .intType()
            .defaultValue(21)
            .description("ftp服务器port")
            .build();

    public static final Cfg.Option<String> user = Cfg.Option.of("vfs.ftp.user")
            .stringType()
            .defaultValue("anonymous")
            .description("ftp认证-用户名")
            .build();
    public static final Cfg.Option<String> passwd = Cfg.Option.of("vfs.ftp.passwd")
            .stringType()
            .defaultValue("")
            .description("ftp认证-密码")
            .build();

    public static final Cfg.Option<Boolean> usePassiveMode = Cfg.Option.of("vfs.ftp.passiveMode")
            .booleanType()
            .defaultValue(true)
            .description("使用被动模式，若为主动模式，连接将由服务端向客户端发送，可能因客户端防火墙等无法访问")
            .build();

    public static final Cfg.Option<String> encoding = Cfg.Option.of("vfs.ftp.encoding")
            .stringType()
            .defaultValue("utf-8") /* GBK 也可 */
            .description("连接所使用的命令编码")
            .build();

    public static final Cfg.Option<Boolean> autodetectUTF8 = Cfg.Option.of("vfs.ftp.autodetectUTF8")
            .booleanType()
            .defaultValue(true)
            .description("自动检测服务器是否有启动使用utf8编码")
            .build();

    public static final Cfg.Option<Integer> controlKeepAliveTimeoutSec = Cfg.Option.of("vfs.ftp.control.alive.timeout")
            .intType()
            .defaultValue(60 * 5)
            .description("超时控制时间间隔（秒）")
            .build();

    public static final Cfg.Option<Integer> connectTimeoutMs = Cfg.Option.of("vfs.ftp.connect.timeout")
            .intType()
            .defaultValue(1000 * 10)
            .description("连接时的超时时间（毫秒）")
            .build();

    public static final Cfg.Option<String> serverTimeZone = Cfg.Option.of("vfs.ftp.serverTimeZone")
            .stringType()
            .defaultValue("Asia/Shanghai")
            .description("ftp服务器时区设置")
            .build();
    public static final Cfg.Option<Integer> transformQueueMaxSize = Cfg.Option.of("vfs.ftp.transform.queue.size")
            .intType()
            .defaultValue(5)
            .description("传输队列大小，最大可同时传输的文件数(vfs-impl-ftp实现中的设置，并非ftp协议本身的设置)")
            .build();

    public static final Cfg.Option<Integer> bufSize = Cfg.Option.of("vfs.ftp.buf.size")
            .intType()
            .defaultValue(VFSDefaultConst.BYTE_BUFFER_SIZE)
            .description("字节传输的缓冲区大小(vfs-impl-ftp实现中的设置，并非ftp协议本身的设置)")
            .build();

    /*
    当隐藏隐藏的文件会导致在服务器不支持MLST命令的情况下 tryGetFile行为异常，遂不允许隐藏隐藏的文件
    @Deprecated
    public static final Config.Option<Boolean> showHiddenFile = Config.Option.of("vfs.conf.ftp.showHiddenFile")
            .booleanType()
            .required(false)
            .defaultValue(true)
            .description("是否显示隐藏文件")
            .build();
    */
}
