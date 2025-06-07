package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.db.func.FnRSRowMapping;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;

import java.sql.Connection;

/**
 * <b>池化的数据库连接器（Pooled Database Connector）</b>
 * <p>线程安全，能够管理自己生产的 {@link Connection} 对象生命周期，
 * 相对于仅能提供Connection对象的不可变的 {@link DBC} 类型，该类型在生命周期内内部状态将会有变化<br>
 * 该类型的构造过程应如 {@link DBC} 类型一样，将构造行为委托至 {@link DBCFactory}
 * <pre>
 *     {@code
 *     PooledDBC pooled = DBCFactory.build(...).pooled();
 *     // 外侧显式获取连接对象并操作，
 *     // 最后应显式调用close或使用try-with-resource关闭，确保归还Conn对象
 *     CompletableFuture.runAsync(() -> {
 *             try (Connection c = pooled.getConn()){
 *                 // do some...
 *             } catch (Exception e) {
 *                 // do some...
 *             }
 *     });
 *     }
 * </pre>
 *
 * @author baifangkual
 * @implNote 当前该类型无连接保活等行为措施，遂当前的连接保活依赖于各数据库JDBC驱动的实现，若JDBC驱动实现中无连接保活
 * 相关策略，则该无连接保活行为
 * @see #close()
 * @see FnResultSetCollector
 * @see FnRSRowMapping
 * @since 2024/7/25 v0.0.7
 */
public interface PooledDBC extends DBC, Closeable {

    /**
     * 从连接池中获取一个连接对象
     * <p>若没有空闲可用连接对象，且连接对象数量已达到 {@link DBCCfgOptions#maxPoolSize}，
     * 则调用该方法的线程将阻塞直到任意一个连接可用</p>
     *
     * @return 一个连接对象
     * @throws Exception 当创建或返回{@link Connection} 过程发生异常时
     */
    @Override
    Connection getConn() throws Exception;

    /**
     * 关闭连接池
     *
     * @throws Exception 当close过程发生异常
     */
    @Override
    void close() throws Exception;

    /**
     * 连接池是否已被关闭
     * <p>已被关闭的连接池不应再使用
     *
     * @return true 已被关闭，反之则未被关闭
     */
    @Override
    boolean isClosed();

}
