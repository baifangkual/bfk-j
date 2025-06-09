package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.exception.DBConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * <b>DBC工厂</b>
 * 工厂类，传入连接参数对象，生成某种数据库的 Database Connector 对象,
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public final class DBCFactory {

    private static final Logger log = LoggerFactory.getLogger(DBCFactory.class);

    private static final List<DBType> SUPPORTED_TYPES;

    static {
        List<String> runtimeLoadingDriver = DriverManager
                .drivers()
                .map(d -> d.getClass().getName())
                .toList();
        // 上述当前有驱动，也不代表当前支持，取交集
        Map<String, DBType> driverTRef = Arrays.stream(DBType.values())
                .filter(t -> t.driver() != null)
                .filter(t -> !t.driver().isBlank())
                .collect(Collectors.toMap(DBType::driver, Function.identity()));
        // 计算其
        List<DBType> types = new ArrayList<>();
        for (Map.Entry<String, DBType> e : driverTRef.entrySet()) {
            String dn = e.getKey();
            DBType dt = e.getValue();
            if (runtimeLoadingDriver.contains(dn)) {
                if (log.isDebugEnabled()) {
                    log.debug("已注册：{} 数据库支持", dt);
                }
                types.add(dt);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("数据库: {} 未在类路径找到驱动类，该类型无法使用，需要驱动类: {}", dt, dt.driver());
                }
            }

        }
        // immutable warp
        SUPPORTED_TYPES = Collections.unmodifiableList(types);
    }

    /**
     * 当前类路径是否有某种数据库类型的驱动类
     *
     * @param type 某种数据库类型
     * @return true 有，false 没有
     */
    public static boolean hasDriver(DBType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    /**
     * 当前支持创建的数据库类型种类枚举，
     * <p>若某类型数据库驱动不在当前运行时的类路径当中，或SPI找不到，则集合中不会有该
     *
     * @return list(...)
     */
    public static List<DBType> supports() {
        return SUPPORTED_TYPES;
    }

    /**
     * 根据给定的配置类信息，创建一个数据库连接器 {@link DBC}
     *
     * @param cfg 连接配置
     * @return {@link DBC}
     */
    public static DBC build(Cfg cfg) throws IllegalDBCCfgException, DBConnectException {
        DBType DBType = cfg.get(DBCCfgOptions.type);
        return DBType.fnNewDBC().apply(cfg);
    }

}
