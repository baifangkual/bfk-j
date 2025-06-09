package io.github.baifangkual.jlib.db.impl;

import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DB;
import io.github.baifangkual.jlib.db.PooledDB;
import io.github.baifangkual.jlib.db.exception.DBConnectException;
import io.github.baifangkual.jlib.db.exception.DriverNotFoundException;
import io.github.baifangkual.jlib.db.impl.abs.TypedJdbcUrlPaddingDBC;
import io.github.baifangkual.jlib.db.impl.pool.ConnPoolDBC;
import io.github.baifangkual.jlib.db.util.PropMapc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * @author baifangkual
 * @since 2025/6/9 v0.1.1
 */
public class SimpleDBImpl implements DB {

    private final Driver driver;
    private final String jdbcUrl;
    private final FnAssertValidConnect FnAssertValidConnect;
    // nullable ================
    private final String user;
    private final String passwd;
    // nullable ================
    private final Map<String, String> jdbcParams;
    // params + user + passwd
    private final Properties fullProp;


    SimpleDBImpl(Driver driver, String jdbcUrl,
                 String nullableUser, String nullablePasswd,
                 Map<String, String> nullableJdbcParams,
                 FnAssertValidConnect FnAssertValidConnect) {
        this.user = nullableUser; // nullable
        this.passwd = nullablePasswd; // nullable
        Properties prop = PropMapc.convert(nullableJdbcParams == null ? Map.of() : nullableJdbcParams);
        if (nullableUser != null) {
            prop.setProperty(TypedJdbcUrlPaddingDBC.DEFAULT_PROP_KEY_USER, nullableUser);
        }
        if (nullablePasswd != null) {
            prop.setProperty(TypedJdbcUrlPaddingDBC.DEFAULT_PROP_KEY_PASSWD, nullablePasswd);
        }
        this.fullProp = prop;
        this.driver = Objects.requireNonNull(driver);
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
        this.jdbcParams = nullableJdbcParams == null ? Map.of() : nullableJdbcParams;
        this.FnAssertValidConnect = Objects.requireNonNull(FnAssertValidConnect);
    }

    public static SimpleDBImpl of(String jdbcUrl,
                                  String nullableUser, String nullablePasswd,
                                  Map<String, String> nullableJdbcParams,
                                  FnAssertValidConnect FnAssertValidConnect) {
        try {
            Driver driver = DriverManager.getDriver(jdbcUrl);
            return of(driver, jdbcUrl, nullableUser, nullablePasswd, nullableJdbcParams, FnAssertValidConnect);
        } catch (SQLException e) { // 不捕获运行时异常
            throw new DriverNotFoundException(Stf.f("use jdbcUrl '{}' find driver failed", jdbcUrl), e);
        }
    }

    public static SimpleDBImpl of(Driver driver,
                                  String jdbcUrl,
                                  String nullableUser, String nullablePasswd,
                                  Map<String, String> nullableJdbcParams,
                                  FnAssertValidConnect FnAssertValidConnect) {
        return new SimpleDBImpl(driver, jdbcUrl, nullableUser, nullablePasswd, nullableJdbcParams, FnAssertValidConnect);
    }


    @Override
    public PooledDB pooled(int maxPoolSize) {
        return new ConnPoolDBC(this, maxPoolSize);
    }

    @Override
    public String jdbcUrl() {
        return jdbcUrl;
    }

    public Optional<String> user() {
        return Optional.ofNullable(user);
    }

    public Optional<String> passwd() {
        return Optional.ofNullable(passwd);
    }

    public Map<String, String> jdbcParams() {
        return jdbcParams;
    }

    public Driver driver() {
        return driver;
    }

    @Override
    public String toString() {
        return Stf.f("{}@{}[driver:{}, jdbcUrl:{}]", this.getClass().getSimpleName(), hashCode(), driver, jdbcUrl);
    }

    @Override
    public SimpleDBImpl assertConnect() throws DBConnectException {
        try (Connection conn = getConn()) {
            fnAssertValidConnect().assertIsValid(conn);
            return this;
        } catch (Exception e) {
            throw new DBConnectException(e);
        }
    }

    public FnAssertValidConnect fnAssertValidConnect() {
        return FnAssertValidConnect;
    }

    @Override
    public Connection getConn() throws SQLException {
        return driver.connect(jdbcUrl, fullProp);
    }
}
