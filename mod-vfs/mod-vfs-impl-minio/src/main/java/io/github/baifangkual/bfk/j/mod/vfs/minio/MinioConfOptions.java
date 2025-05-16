package io.github.baifangkual.bfk.j.mod.vfs.minio;

import io.github.baifangkual.bfk.j.mod.core.conf.Cfg;
import io.github.baifangkual.bfk.j.mod.vfs.VFSDefaultConst;

import java.util.Collections;
import java.util.List;

/**
 * minio vfs 连接配置
 *
 * @author baifangkual
 * @since 2024/9/2 v0.0.5
 */
public class MinioConfOptions {

    private MinioConfOptions() {
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

    public static final Cfg.Option<List<String>> excludeMinioObjNames = Cfg.Option.of("vfs.minio.filter.names")
            .<List<String>>type()
            .defaultValue(Collections.emptyList())
            .description("过滤某些目录实体的名称，使其不体现在vfs中")
            .build();

    public static final Cfg.Option<MinioDirectoryActionStrategy> dirActionStrategy = Cfg.Option.of("vfs.minio.dirActionStrategy")
            .type(MinioDirectoryActionStrategy.class)
            .defaultValue(MinioDirectoryActionStrategy.MINIO_NATIVE)
            .description("对minio文件夹的行为策略配置")
            .build();

    public static final Cfg.Option<Integer> bufSize = Cfg.Option.of("vfs.minio.buf.size")
            .intType()
            .defaultValue(VFSDefaultConst.BYTE_BUFFER_SIZE)
            .description("minio putObject 缓冲区大小")
            .build();


}
