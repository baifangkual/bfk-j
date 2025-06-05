package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.utils.PropertiesMapConverter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author baifangkual
 * create time 2024/7/11
 */
public abstract class DefaultJDBCUrlFormatDataSource extends AbstractDataSource {

    private static final String DEFAULT_CHECK_CONN_SQL = "SELECT 1";
    private static final String DEFAULT_PROP_KEY_USER = "user";
    private static final String DEFAULT_PROP_KEY_PASSWD = "password";

    public DefaultJDBCUrlFormatDataSource(Cfg connCfg) {
        super(connCfg);
    }


    /*
     * jdbc:mysql://localhost:3306/test
     * jdbc:postgresql://localhost:5432/postgres
     * jdbc:dm://localhost:5236
     * jdbc:sqlserver://localhost:1433
     * jdbc:oracle:thin:@localhost:1521/xepdb1 sid
     * jdbc:oracle:thin:@//localhost:1521/orcl serviceName
     * jdbc:sqlite:test.db || support?
     * jdbc:kingbase8://localhost:54321/db_test
     * jdbc:hive2://localhost:10000/default
     * */


    protected abstract String buildingJdbcUrl(Cfg Cfg);

    @Override
    public String getJdbcUrl() {
        return buildingJdbcUrl(connConfig);
    }

    @Override
    public Connection getConnection() throws Exception {
        String url = buildingJdbcUrl(connConfig);
        // 不应修改原对象，仅只读
        Map<String, String> params = new HashMap<>(connConfig.getOrDefault(ConnConfOptions.JDBC_PARAMS_OTHER));
        // tod o 部分数据库或部分连接类型并不需要用户名和密码 遂该处找不到不应强制抛出异常
        connConfig.tryGet(ConnConfOptions.USER)
                .ifPresent(un -> params.put(DEFAULT_PROP_KEY_USER, un));
        connConfig.tryGet(ConnConfOptions.PASSWD)
                .ifPresent(pw -> params.put(DEFAULT_PROP_KEY_PASSWD, pw));
        // conn prop
        Properties prop = PropertiesMapConverter.convert(params);
        // tod o 额外参数设定，应当在别处 ？ 这非与数据源类型相关的专有参数，而是连接相关的参数
        DriverManager.setLoginTimeout(connConfig.getOrDefault(ConnConfOptions.CONN_TIMEOUT_SEC));
        return DriverManager.getConnection(url, prop);
    }

    // todo 调整检查条件和sql
    @Override
    public void checkConnection() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(DEFAULT_CHECK_CONN_SQL)) {
            while (rs.next()) {
                int i = rs.getInt(1);
                if (i == 1) return;
            }
        }
    }
}
