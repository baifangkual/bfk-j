package io.github.baifangkual.jlib.vfs.fs.conf;

import io.github.baifangkual.jlib.core.conf.Cfg;

/**
 * 本地文件系统 vfs 配置项
 *
 * @author baifangkual
 * @since 2025/6/10
 */
@Deprecated // 未完成
public class LocalCfgOptions {

    private LocalCfgOptions() {
        throw new AssertionError("not instantiable");
    }

    public static final Cfg.Option<String> rootOf = Cfg.Option.of("vfs.fs.root")
            .stringType()
            .defaultValue("/")
            .description("使用哪里作为VFS的根目录，默认值为'/', 即表示使用可访问到的本地文件系统的根目录，" +
                         "在windows端，'/' 表示为所有盘符的父级目录")
            .build();


}
