package io.github.baifangkual.jlib.db;


import io.github.baifangkual.jlib.core.conf.Cfg;

import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/7/12
 */
public class DBCCfgOptions {
    private DBCCfgOptions() {
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
            .defaultValue(10)
            .description("连接池默认最大连接对象数量")
            .build();


    public static final Cfg.Option<String> USER = Cfg.Option.of("jdbc.auth.user")
            .stringType() // 没有默认行为“root”， 因为不是所有数据库默认user都是root
            .description("数据库登录凭证")
            .build();
    public static final Cfg.Option<String> PASSWD = Cfg.Option.of("jdbc.auth.passwd")
            .stringType()
            .description("数据库登陆凭证")
            .build();
    public static final Cfg.Option<DBType> DS_TYPE = Cfg.Option.of("jdbc.datasource.type")
            .type(DBType.class)
            .description("数据源类型")
            .build();
    public static final Cfg.Option<String> HOST = Cfg.Option.of("jdbc.host")
            .stringType()
            .description("JDBC连接数据库的HOST，应有该")
            .build();
    public static final Cfg.Option<Integer> PORT = Cfg.Option.of("jdbc.port")
            .intType()
            .description("JDBC连接数据库的PORT")
            .build();
    public static final Cfg.Option<String> DB = Cfg.Option.of("jdbc.database")
            .stringType()
            .description("JDBC连接的数据库的名称")
            .build();
    public static final Cfg.Option<String> SCHEMA = Cfg.Option.of("jdbc.schema")
            .stringType()
            .description("部分数据库有该概念，部分数据库无")
            .build();
    public static final Cfg.Option<Map<String, String>> JDBC_PARAMS_OTHER = Cfg.Option.of("jdbc.other.params")
            .<Map<String, String>>type()
            .defaultValue(Map.of())
            .description("JDBC连接的额外参数，不同数据库有不同")
            .build();
    public static final Cfg.Option<Integer> CONN_TIMEOUT_SEC = Cfg.Option.of("jdbc.connect.timeout.seconds")
            .intType()
            .defaultValue(10)
            .description("JDBC在尝试获取数据源连接时等待的最大连接超时时间")
            .build();


}
