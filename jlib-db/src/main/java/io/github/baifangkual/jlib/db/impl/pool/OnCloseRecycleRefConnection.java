package io.github.baifangkual.jlib.db.impl.pool;

import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.exception.DBConnectException;

import java.sql.*;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 连接池中使用的 Conn 代理
 * 代理{@link Connection}对象，当{@link Connection#close()}发生时，将自己返回给原有引用地方，
 * 该应该作为连接池中对象而不是直接使用, 该对象的生命周期与{@link ConnPoolDBC}相关<br>
 * 该对象实际是由{@link Poolable}类型实现对象创建的，当前也即{@link ConnPoolDBC}
 * <p>多个线程共享使用该对象仍然不是线程安全的，线程应通过 {@link ConnPoolDBC} 获取该类型实例，
 * 这样的获取方式可以保证不同线程不会持有同一个对象的引用
 *
 * @author baifangkual
 * @since 2024/7/26
 */
@SuppressWarnings("SqlSourceToSinkFlow")
public class OnCloseRecycleRefConnection implements Connection, Poolable.Borrowable {

    private static final String CLOSED_CONNECTION = "Connection is closed";
    private static final String UN_OPEN_CONNECTION = "un open connection";

    private final int id;
    private final Connection delegate;
    private final ConnPoolDBC pool;
    // 多个线程同时共享使用该类型时，无法保证下字段可见性和原子性
    private boolean inUsed = false;
    private boolean isAutoCommit;
    private long lastUsedTimeMillis;

    OnCloseRecycleRefConnection(int id,
                                Connection delegate,
                                ConnPoolDBC pool,
                                boolean isAutoCommit) {
        Objects.requireNonNull(delegate);
        if (delegate instanceof Poolable.Borrowable) {
            throw new IllegalArgumentException("Connection is a recyclable connection");
        }
        this.id = id;
        this.delegate = delegate;
        this.pool = pool;
        this.isAutoCommit = isAutoCommit;
    }

    boolean proxyIsAutoCommit() {
        return isAutoCommit;
    }

    long lastUsedTimeMillis() {
        return lastUsedTimeMillis;
    }

    /**
     * 修改占用标志位，触发pool.recycle方法，将自己回收
     */
    @Override
    public void recycleSelf() {
        if (inUsed) {
            inUsed = false;
            lastUsedTimeMillis = System.currentTimeMillis(); // 系统调用，归还时更新
            pool.recycle(this);
        } else throw new IllegalStateException(CLOSED_CONNECTION);
    }

    /**
     * 由pool借用前调用，更改标志位，方法作用域为包，不应pub
     */
    void borrowBef() {
        // inUse setting
        if (inUsed) {
            throw new IllegalStateException(UN_OPEN_CONNECTION);
        }
        inUsed = true;
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
    int connId() {
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
        return id == that.id && this.pool.instanceId() == that.pool.instanceId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pool.instanceId());
    }

    @Override
    public String toString() {
        // 该内，不应在调用pool的toString，防止Pool的ToString等因循环引用...
        return Stf.f("OnCloseRecycleRefConnection[id:{}, ref:{}, inUsed:{}]", id, delegate, inUsed);
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
        return !inUsed;
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
        this.isAutoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return this.isAutoCommit; // 无需查看 delegate 的
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
        if (isClosed()) throw new DBConnectException(CLOSED_CONNECTION);
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (isClosed()) throw new DBConnectException(CLOSED_CONNECTION);
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

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.unwrap(iface);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (isClosed()) throw new SQLException(CLOSED_CONNECTION);
        return delegate.isWrapperFor(iface);
    }

}
