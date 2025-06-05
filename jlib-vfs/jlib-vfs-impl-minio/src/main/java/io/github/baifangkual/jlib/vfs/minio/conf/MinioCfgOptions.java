package io.github.baifangkual.jlib.vfs.minio.conf;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.vfs.VFSDefaults;
import io.github.baifangkual.jlib.vfs.VFSFactory;
import io.github.baifangkual.jlib.vfs.VFS;
import io.github.baifangkual.jlib.vfs.minio.MinioDirectoryActionStrategy;

/**
 * minio vfs 连接配置<br>
 * 可通过{@link VFSFactory}创建{@link VFS}
 *
 * @author baifangkual
 * @since 2024/9/2 v0.0.5
 */
public class MinioCfgOptions {

    private MinioCfgOptions() {
        throw new UnsupportedOperationException("utility class");
    }

    public static final Cfg.Option<String> host = Cfg.Option.of("vfs.minio.host")
            .stringType()
            .description("minio 服务 ipv4/v6 或 域名")
            .build();

    public static final Cfg.Option<Integer> port = Cfg.Option.of("vfs.minio.port")
            .intType()
            .defaultValue(9000)
            .description("minio 服务 请求监听端口")
            .build();

    public static final Cfg.Option<String> bucket = Cfg.Option.of("vfs.minio.bucket.use")
            .stringType()
            .description("使用哪个bucket，在以bucket为根的虚拟文件系统中该参数为必要参数")
            .notFoundValueMsg("未设置minio bucket")
            .build();

    public static final Cfg.Option<String> accessKey = Cfg.Option.of("vfs.minio.auth.acc.key")
            .stringType()
            .description("minio认证accessKey")
            .build();

    public static final Cfg.Option<String> secretKey = Cfg.Option.of("vfs.minio.auth.sec.key")
            .stringType()
            .description("minio认证secretKey")
            .build();

    public static final Cfg.Option<Boolean> useHttpsSecure = Cfg.Option.of("vfs.minio.secure.useHttpsSecure")
            .booleanType()
            .defaultValue(false)
            .description("是否使用 TLS https安全连接")
            .build();

    public static final Cfg.Option<MinioDirectoryActionStrategy> dirActionStrategy = Cfg.Option.of("vfs.minio.dirActionStrategy")
            .type(MinioDirectoryActionStrategy.class)
            .defaultValue(MinioDirectoryActionStrategy.MINIO_NATIVE)
            .description("对minio文件夹的行为策略配置")
            .build();

    public static final Cfg.Option<Integer> bufSize = Cfg.Option.of("vfs.minio.buf.size")
            .intType()
            .defaultValue(VFSDefaults.BYTE_BUFFER_SIZE)
            .description("minio putObject 缓冲区大小")
            .build();


}
