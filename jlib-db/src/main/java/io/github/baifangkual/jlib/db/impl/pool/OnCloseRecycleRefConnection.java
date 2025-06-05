package io.github.baifangkual.jlib.db.impl.pool;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.DataSourceConnectionFailException;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.trait.Pool;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;
import lombok.NonNull;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author baifangkual
 * create time 2024/7/26
 * <p>
 * 代理{@link Connection}对象，当{@link Connection#close()}发生时，将自己返回给原有引用地方，
 * 该应该作为连接池中对象而不是直接使用,当前（20240729）该对象的生命周期与{@link ConnectionPoolProxyDataSource}相关<br>
 * 该对象实际是由{@link Pool}类型实现对象创建的，当前也即{@link ConnectionPoolProxyDataSource}
 */
public class OnCloseRecycleRefConnection implements Connection, Pool.Borrowable {

    private static final String CLOSED_CONNECTION = "Connection is closed";
    private static final String UN_OPEN_CONNECTION = "un open connection";

    private final int id;
    private final Connection delegate;
    private final Pool<OnCloseRecycleRefConnection> pool;
    private final AtomicBoolean inUse = new AtomicBoolean(false);

    OnCloseRecycleRefConnection(int id,
                                @NonNull Connection delegate,
                                @NonNull Pool<OnCloseRecycleRefConnection> pool) {
        if (delegate instanceof Pool.Borrowable) {
            throw new IllegalArgumentException("Connection is a recyclable connection");
        }
        this.id = id;
        this.delegate = delegate;
        this.pool = pool;
    }

    /**
     * 修改占用标志位，触发pool.recycle方法，将自己回收
     */
    @Override
    public void recycleSelf() {
        // 自己修改状态，第一个线程
        if (inUse.compareAndSet(true, false)) {
            pool.recycle(this);
        } else throw new IllegalStateException(CLOSED_CONNECTION);
    }

    /**
     * 由pool借用前调用，更改标志位，方法作用域为包，不应pub
     */
    void borrowBef() {
        // inUse setting
        if (!inUse.compareAndSet(false, true)) {
            throw new IllegalStateException(UN_OPEN_CONNECTION);
        }
    }

    /**
     * 获取该实际的Conn对象，方法作用域为包，不应pub
     *
     * @return conn
     */
    Connection realConnection() {
        return delegate;
    }

    /**
     * 获取该的唯一标识，方法作用域为包，不应pub
     *
     * @return id for this
     */
    int getId() {
        return id;
    }

    /**
     * 仅限 {@link java.util.concurrent.BlockingDeque#contains(Object)} 使用
     *
     * @param o other
     * @return eq or
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OnCloseRecycleRefConnection that)) return false;
        return id == that.id;
    }

    /**
     * 仅限{@link java.util.concurrent.BlockingDeque#contains(Object)} 使用
     *
     * @return hash
     */
    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return Stf.f("OnCloseRecycleRefConnection[id:{}, ref:{}, inUse:{}]", id, delegate, inUse.get());
    }

    // proxy ===============================

    /**
     * 该类型的close行为实际为将自己回收，而非真正关闭连接
     */
    @Override
    public void close() {
        recycleSelf();
    }

    /**
     * 该类型的isClosed行为实际为标志位是否在被使用，而非真正的是否已经被关闭
     *
     * @return 当调用该的close方法后，该返回值为true，否则为false
     */
    @Override
    public boolean isClosed() {
        return !inUse.get();
    }
    // proxy ===============================

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getMetaData();
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.rollback();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (isClosed()) throw new DataSourceConnectionFailException(CLOSED_CONNECTION);
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (isClosed()) throw new DataSourceConnectionFailException(CLOSED_CONNECTION);
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.getNetworkTimeout();
    }

    @Override
    public void beginRequest() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.beginRequest();
    }

    @Override
    public void endRequest() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.endRequest();
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.setShardingKeyIfValid(shardingKey, superShardingKey, timeout);
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.setShardingKeyIfValid(shardingKey, timeout);
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setShardingKey(shardingKey, superShardingKey);
    }

    @Override
    public void setShardingKey(ShardingKey shardingKey) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        delegate.setShardingKey(shardingKey);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.isWrapperFor(iface);
    }

}
