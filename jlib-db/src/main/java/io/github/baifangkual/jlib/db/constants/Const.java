package io.github.baifangkual.jlib.db.constants;


import io.github.baifangkual.jlib.core.conf.Cfg;

/**
 * @author baifangkual
 * create time 2024/7/16
 */
public class Const {
    private Const() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final Cfg.Option<Long> PAGE_NO = Cfg.Option.of("pageNo")
            .longType()
            .defaultValue(1L)
            .description("分页参数-页码")
            .build();
    public static final Cfg.Option<Long> PAGE_SIZE = Cfg.Option.of("pageSize")
            .longType()
            .defaultValue(100L)
            .description("分页参数-页大小")
            .build();

    public static final Cfg.Option<Cfg> CONN_POOL_CONFIG = Cfg.Option.of("conn.pool.Cfg")
            .defaultValue(Cfg.readonlyEmpty())
            .description("连接池的配置对象")
            .build();
    public static final Cfg.Option<Integer> CONN_POOL_MAX_SIZE = Cfg.Option.of("conn.pool.Cfg.maxSize")
            .defaultValue(5)
            .description("连接池默认最大连接对象数量")
            .build();

}
