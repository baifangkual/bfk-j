package io.github.baifangkual.jlib.db.utils;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.constants.Const;
import io.github.baifangkual.jlib.db.enums.DSType;
import io.github.baifangkual.jlib.db.enums.URLType;
import io.github.baifangkual.jlib.db.impl.pool.ConnectionPoolProxyDataSource;
import io.github.baifangkual.jlib.db.trait.CloseableDataSource;
import io.github.baifangkual.jlib.db.trait.DataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.DriverManager;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author baifangkual
 * create time 2024/7/11
 * <p>
 * 工厂类，传入连接参数对象，生成某种数据库的 database 对象,
 */
@Slf4j
public final class DataSourceCreators {


    private static final List<DSType> SUPPORTED_TYPES;

    static {
        // 此处寻找当前运行时的driverClass，不应该由Class.forName，应当委托至DriverManager完整验证
        List<String> runtimeLoadingDriver = DriverManager.drivers().map(d -> d.getClass().getName())
                .toList();
        // 上述当前有驱动，也不代表当前支持，取交集
        Map<String, DSType> driverTRef = Arrays.stream(DSType.values())
                .filter(t -> t.getDriver() != null)
                .filter(t -> !t.getDriver().isBlank())
                .collect(Collectors.toMap(DSType::getDriver, Function.identity()));
        // 计算其
        List<DSType> types = new ArrayList<>();
        for (Map.Entry<String, DSType> e : driverTRef.entrySet()) {
            String dn = e.getKey();
            DSType dt = e.getValue();
            if (runtimeLoadingDriver.contains(dn)) {
                log.info("已注册：{} 数据库支持", dt);
                types.add(dt);
            } else {
                log.info("数据库: {} 未在类路径找到驱动类，该类型无法使用，需要驱动类: {}", dt, dt.getDriver());
            }

        }
        // immutable warp
        SUPPORTED_TYPES = Collections.unmodifiableList(types);
    }

    /**
     * 当前类路径是否有某种数据库类型的驱动类
     *
     * @param t 某种数据库类型
     * @return true 有，false 没有
     */
    public static boolean hasDriver(DSType t) {
        return SUPPORTED_TYPES.contains(t);
    }

    /**
     * 当前支持创建的数据库类型种类枚举，若某类型数据库驱动不在当前运行时的类路径当中，SPI找不到，集合中不会有该
     *
     * @return list[datasource type...]
     */
    public static List<DSType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * 根据给定的配置类信息，创建一个某种类型的数据源{@link DataSource}对象，该对象可返回{@link java.sql.Connection}
     *
     * @param config 连接配置
     * @return {@link DataSource}
     */
    public static DataSource create(Cfg config) {
        DSType dsType = config.tryGet(ConnConfOptions.DS_TYPE)
                .orElseThrow(() -> new IllegalArgumentException("参数中缺少数据库类型"));
        URLType urlType = config.getOrDefault(ConnConfOptions.JDBC_URL_TYPE);
        Err.realIf(!SUPPORTED_TYPES.contains(dsType), IllegalArgumentException::new,
                "{} 类型数据库缺少驱动，非法参数", dsType);
        Err.realIf(!dsType.isSupport(urlType), IllegalArgumentException::new,
                "{} 不支持该类型的 urlType，urlType: {}", dsType, urlType);
        return dsType.getDatasourceConstructor().apply(config);
    }

    /**
     * 根据给定的连接信息，创建{@link CloseableDataSource}该类型继承自{@link DataSource}，遂可执行继承的方法和额外的行为，
     * CloseableDataSource都是有状态对象，经典的行为为连接池，该实现使用{@link ConnectionPoolProxyDataSource}
     *
     * @param config      连接配置
     * @param maxPoolSize 连接池最大连接数
     * @return impl {@link CloseableDataSource}
     */
    public static CloseableDataSource createConnPool(Cfg config, int maxPoolSize) {
        Cfg pc = config.tryGet(Const.CONN_POOL_CONFIG).orElse(Cfg.newCfg());
        return createConnPool(config.reset(Const.CONN_POOL_CONFIG, pc.reset(Const.CONN_POOL_MAX_SIZE, maxPoolSize)));
    }

    /**
     * @see #createConnPool(Cfg, int)
     */
    public static CloseableDataSource createConnPool(Cfg config) {
        return new ConnectionPoolProxyDataSource(config);
    }

    public static CloseableDataSource poolWarp(DataSource dataSource, int maxPoolSize) {
        return new ConnectionPoolProxyDataSource(dataSource, maxPoolSize);
    }


}
