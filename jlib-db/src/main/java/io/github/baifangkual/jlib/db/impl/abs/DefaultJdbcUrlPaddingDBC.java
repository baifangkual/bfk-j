package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.util.PropMapc;

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
public abstract class DefaultJdbcUrlPaddingDBC extends AbstractDBC {

    @SuppressWarnings("SqlNoDataSourceInspection")
    private static final String DEFAULT_CHECK_CONN_SQL = "SELECT 1";
    private static final String DEFAULT_PROP_KEY_USER = "user";
    private static final String DEFAULT_PROP_KEY_PASSWD = "password";

    private final String jdbcUrl;
    private final Properties prop;

    public DefaultJdbcUrlPaddingDBC(Cfg cfg) {
        super(cfg);
        Cfg readOnlyCfg = cfg();
        String jdbcUrl = buildingJdbcUrl(readOnlyCfg);
        // 不应修改原对象，仅只读
        Map<String, String> params = new HashMap<>(readOnlyCfg.getOrDefault(DBCCfgOptions.JDBC_PARAMS_OTHER));
        // tod o 部分数据库或部分连接类型并不需要用户名和密码 遂该处找不到不应强制抛出异常
        readOnlyCfg.tryGet(DBCCfgOptions.USER)
                .ifPresent(un -> params.put(DEFAULT_PROP_KEY_USER, un));
        readOnlyCfg.tryGet(DBCCfgOptions.PASSWD)
                .ifPresent(pw -> params.put(DEFAULT_PROP_KEY_PASSWD, pw));
        Properties prop = PropMapc.convert(params);
        // tod o 额外参数设定，应当在别处 ？ 这非与数据源类型相关的专有参数，而是连接相关的参数
        DriverManager.setLoginTimeout(readOnlyCfg.getOrDefault(DBCCfgOptions.CONN_TIMEOUT_SEC));
        this.jdbcUrl = jdbcUrl;
        this.prop = prop;
    }


    protected abstract String buildingJdbcUrl(Cfg Cfg);

    @Override
    public Connection getConn() throws Exception {
        return DriverManager.getConnection(jdbcUrl, prop);
    }

    @Override
    public void checkConn() throws Exception {
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(DEFAULT_CHECK_CONN_SQL)) {
            while (rs.next()) {
                int i = rs.getInt(1);
                if (i == 1) return;
            }
        }
    }
}
