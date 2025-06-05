package io.github.baifangkual.jlib.db.enums;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.impl.ds.MysqlDataSource;
import io.github.baifangkual.jlib.db.impl.ds.OracleDataSource;
import io.github.baifangkual.jlib.db.impl.ds.PostgresqlDataSource;
import io.github.baifangkual.jlib.db.impl.ds.SqlServerDataSource;
import io.github.baifangkual.jlib.db.trait.DataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;

/**
 * @author baifangkual
 * create time 2024/7/10
 * 支持的数据源类型
 */
@Getter
@RequiredArgsConstructor
public enum DSType {

    MYSQL(
            "com.mysql.cj.jdbc.Driver",
            List.of(URLType.JDBC_DEFAULT),
            MysqlDataSource::new
    ),
    POSTGRESQL(
            "org.postgresql.Driver",
            List.of(URLType.JDBC_DEFAULT),
            PostgresqlDataSource::new
    ),
    ORACLE(
            "oracle.jdbc.OracleDriver",
            List.of(URLType.JDBC_DEFAULT, URLType.JDBC_ORACLE_SERVICE_NAME, URLType.JDBC_ORACLE_SID),
            // to do 20240923 oracle 在 支持的url类型这里加入了 jdbc_default，
            //  当oracle 为 jdbc default，其行为应如同 service_name
            //  即oracle 的 url类型默认行为 为 service_name
            OracleDataSource::new
    ),
    SQL_SERVER(
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            List.of(URLType.JDBC_DEFAULT),
            SqlServerDataSource::new
    ),
    ;
    //... other impl

    static {
        // static check
        for (DSType t : DSType.values()) {
            if (t.driver == null
                || t.supportUrlTypes == null
                || t.datasourceConstructor == null)
                throw new IllegalStateException(Stf.f("未定义数据库 {} 的 {} 信息", t, DSType.class.getName()));
        }
    }

    /**
     * 数据源驱动类名
     */
    private final String driver;
    /**
     * 数据源所支持的JDBC URL 类型
     */
    private final List<URLType> supportUrlTypes;
    /**
     * 数据源 {@link DataSource} 构造函数引用
     */
    private final Function<Cfg, DataSource> datasourceConstructor;


    // use
    public boolean isSupport(URLType urlType) {
        return supportUrlTypes.contains(urlType);
    }


}
