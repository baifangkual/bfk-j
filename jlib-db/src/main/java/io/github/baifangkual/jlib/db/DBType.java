package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.impl.ds.MysqlDBC;
import io.github.baifangkual.jlib.db.impl.ds.Oracle11gServerNameJdbcUrlDBC;
import io.github.baifangkual.jlib.db.impl.ds.PostgresqlDBC;
import io.github.baifangkual.jlib.db.impl.ds.SqlServerDBC;

import java.util.function.Function;

/**
 * {@link DBC} 支持的数据源类型
 *
 * @author baifangkual
 * @since 2024/7/10
 */
public enum DBType {

    mysql(
            "com.mysql.cj.jdbc.Driver",
            MysqlDBC::new
    ),
    postgresql(
            "org.postgresql.Driver",
            PostgresqlDBC::new
    ),
    oracle11g( // oracle 类型数据库仅在 oracle11g 版本上测试了，遂保守仅支持11g
            "oracle.jdbc.OracleDriver",
            Oracle11gServerNameJdbcUrlDBC::new
    ),
    sqlServer(
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            SqlServerDBC::new
    ),
    ;

    /**
     * 数据源驱动类名
     */
    private final String driver;
    /**
     * 数据源 {@link DBC} 构造函数引用
     */
    private final Function<Cfg, DBC> fnNewDBC;

    DBType(String driver, Function<Cfg, DBC> fnNewDBC) {
        this.driver = driver;
        this.fnNewDBC = fnNewDBC;
    }

    public Function<Cfg, DBC> fnNewDBC() {
        return fnNewDBC;
    }

    public String driver() {
        return driver;
    }


}
