package io.github.baifangkual.jlib.db.impl.abs;


import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DB;
import io.github.baifangkual.jlib.db.DBC;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.DBType;
import io.github.baifangkual.jlib.db.exception.DBConnectException;
import io.github.baifangkual.jlib.db.util.PropMapc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * 抽象类，封装参数到 jdbcUrl 和 Properties 过程
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public abstract class TypedJdbcUrlPaddingDBC extends BaseDBC {

    public static final String SQL_SELECT_1 = "SELECT 1";
    public static final String DEFAULT_PROP_KEY_USER = "user";
    public static final String DEFAULT_PROP_KEY_PASSWD = "password";

    // user and passwd or not in params
    @SuppressWarnings("FieldCanBeLocal")
    private final Map<String, String> jdbcParams;
    private final String jdbcUrl;
    @SuppressWarnings("FieldCanBeLocal")
    private final Properties fullProp;
    private final DB dbInstance;
    private final DBType dbType;


    // nullable =======================
    @SuppressWarnings("FieldCanBeLocal")
    private final String user;
    @SuppressWarnings("FieldCanBeLocal")
    private final String passwd;
    @SuppressWarnings("FieldCanBeLocal")
    private final String db;
    @SuppressWarnings("FieldCanBeLocal")
    private final String schema;
    // nullable =======================


    public TypedJdbcUrlPaddingDBC(Cfg cfg) {
        super(cfg);
        // tod o 额外参数设定，应当在别处 ？ 这非与数据源类型相关的专有参数，而是连接相关的参数
        // JDBC 的 DriverManager 只提供了静态方法 setLoginTimeout，这会相互影响 不同 DBC 构建过程的设定
        // 尤其在多线程环境下，发现不同的数据库提供商提供了不同的该参数配置可设定到 prop中
        // 遂后续可修改为 放置在 prop中的 timeOut
        Cfg readOnlyCfg = readonlyCfg();
        // 不应修改原对象，仅只读
        Map<String, String> jdbcOtherParams = readOnlyCfg.getOrDefault(DBCCfgOptions.jdbcOtherParams);
        Map<String, String> params = new HashMap<>(jdbcOtherParams);
        // tod o 部分数据库或部分连接类型并不需要用户名和密码 遂该处找不到不应强制抛出异常
        Optional<String> userOpt = readOnlyCfg.tryGet(DBCCfgOptions.user);
        Optional<String> passwdOpt = readOnlyCfg.tryGet(DBCCfgOptions.passwd);
        Optional<String> dbOpt = readOnlyCfg.tryGet(DBCCfgOptions.db);
        Optional<String> schemaOpt = readOnlyCfg.tryGet(DBCCfgOptions.schema);
        userOpt.ifPresent(un -> params.put(DEFAULT_PROP_KEY_USER, un));
        passwdOpt.ifPresent(pw -> params.put(DEFAULT_PROP_KEY_PASSWD, pw));
        DriverManager.setLoginTimeout(readOnlyCfg.getOrDefault(DBCCfgOptions.connTimeoutSecond));
        this.user = userOpt.orElse(null);
        this.passwd = passwdOpt.orElse(null);
        this.db = dbOpt.orElse(null);
        this.schema = schemaOpt.orElse(null);
        this.jdbcParams = jdbcOtherParams;
        this.fullProp = PropMapc.convert(params);
        this.dbType = readonlyCfg().get(DBCCfgOptions.type);
        this.jdbcUrl = buildingJdbcUrl(readOnlyCfg);
        this.dbInstance = DB.simple(jdbcUrl, user, passwd, jdbcOtherParams, fnAssertValidConnect());
    }

    @Override
    public DBC assertConnect() throws DBConnectException {
        this.dbInstance.assertConnect();
        return this;
    }

    public DBType type() {
        return dbType;
    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    protected abstract String buildingJdbcUrl(Cfg readonlyCfg);

    @Override
    public Connection getConn() throws SQLException {
        return this.dbInstance.getConn();
    }

}
