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
@SuppressWarnings("LombokGetterMayBeUsed")
public enum DBType {

    MYSQL(
            "com.mysql.cj.jdbc.Driver",
            MysqlDBC::new
    ),
    POSTGRESQL(
            "org.postgresql.Driver",
            PostgresqlDBC::new
    ),
    ORACLE(
            "oracle.jdbc.OracleDriver",
            Oracle11gServerNameJdbcUrlDBC::new
    ),
    SQL_SERVER(
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
    private final Function<Cfg, DBC> fnDBCConstructor;

    DBType(String driver, Function<Cfg, DBC> fnDBCConstructor) {
        this.driver = driver;
        this.fnDBCConstructor = fnDBCConstructor;
    }

    public Function<Cfg, DBC> getFnDBCConstructor() {
        return fnDBCConstructor;
    }

    public String getDriver() {
        return driver;
    }


}
