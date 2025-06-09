package io.github.baifangkual.jlib.db;


import io.github.baifangkual.jlib.core.conf.Cfg;

import java.time.Duration;
import java.util.Map;

/**
 * jdbc 数据库连接参数及连接池参数等
 *
 * @author baifangkual
 * @since 2024/7/12
 */
public class DBCCfgOptions {
    private DBCCfgOptions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final Cfg.Option<String> user = Cfg.Option.of("jdbc.auth.user")
            .stringType() // 没有默认行为“root”， 因为不是所有数据库默认user都是root
            .description("数据库登录凭证")
            .build();
    public static final Cfg.Option<String> passwd = Cfg.Option.of("jdbc.auth.passwd")
            .stringType()
            .description("数据库登陆凭证")
            .build();
    public static final Cfg.Option<DBType> type = Cfg.Option.of("jdbc.db.type")
            .type(DBType.class)
            .description("数据源类型")
            .build();
    public static final Cfg.Option<String> host = Cfg.Option.of("jdbc.host")
            .stringType()
            .description("JDBC连接数据库的HOST，应有该")
            .build();
    public static final Cfg.Option<Integer> port = Cfg.Option.of("jdbc.port")
            .intType()
            .description("JDBC连接数据库的PORT")
            .build();
    public static final Cfg.Option<String> db = Cfg.Option.of("jdbc.database")
            .stringType()
            .description("JDBC连接的数据库的名称")
            .build();
    public static final Cfg.Option<String> schema = Cfg.Option.of("jdbc.schema")
            .stringType()
            .description("部分数据库有该概念，部分数据库无")
            .build();
    public static final Cfg.Option<Map<String, String>> jdbcOtherParams = Cfg.Option.of("jdbc.other.params")
            .<Map<String, String>>type()
            .defaultValue(Map.of())
            .description("JDBC连接的额外参数，不同数据库有不同")
            .build();
    public static final Cfg.Option<Integer> connTimeoutSecond = Cfg.Option.of("jdbc.connect.timeout.seconds")
            .intType()
            .defaultValue(10)
            .description("JDBC在尝试获取数据源连接时等待的最大连接超时时间-单位秒")
            .build();

    // ============ POOLED_DBC =====================
    public static final Cfg.Option<Integer> poolMaxSize = Cfg.Option
            .of("pool.maxSize")
            .defaultValue(Runtime.getRuntime().availableProcessors())
            .description("连接池最大连接数, 默认使用当前设备处理器数量")
            .build();
    public static final Cfg.Option<Duration> poolCheckConnAliveInterval = Cfg.Option
            .of("pool.CheckAliveConnInterval")
            .<Duration>type()
            .defaultValue(Duration.ofSeconds(60)) // 一般情况下够用了，过小会导致每次借用时频繁的检查
            .description("连接池检查连接对象是否可用的时间间隔，" +
                         "(数据库端可能在一定时间后会断开空闲会话，该间隔配置应小于数据库端配置)")
            .build();
    public static final Cfg.Option<Duration> poolMaxWaitBorrowInterval = Cfg.Option
            .of("pool.MaxWaitBorrowInterval")
            .<Duration>type()
            .fallbackOf(poolCheckConnAliveInterval)
            .description("线程等待借用连接对象的最大时间，若等待超时，则等待的线程将抛出异常，" +
                         "默认值与poolCheckConnAliveInterval一致")
            .build();
    public static final Cfg.Option<Duration> poolOnCloseWaitAllConnRecycleInterval = Cfg.Option
            .of("pool.OnCloseWaitAllConnRecycleInterval")
            .<Duration>type()
            .fallbackOf(poolCheckConnAliveInterval)
            .description("连接池在关闭时等待所有连接对象回收的最大时间，若等待超时，则强制关闭所有当前连接池已回收和借出的连接对象，" +
                         "默认值与poolCheckConnAliveInterval一致")
            .build();

    // ============ POOLED_DBC =====================


}
