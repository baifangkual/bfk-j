package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.db.exception.DBConnectException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <b>池化的数据库连接器（Pooled Database Connector）</b>
 * <p>相较于 {@link PooledDB}，该类型实例提供了数据库的部分元数据</p>
 * <p>线程安全，能够管理自己生产的 {@link Connection} 对象生命周期，
 * 相对于仅能提供Connection对象的不可变的 {@link DBC} 类型，该类型在生命周期内内部状态将会有变化<br>
 * 该类型的构造过程应如 {@link DBC} 类型一样，将构造行为委托至 {@link DBCFactory}
 * <p>使用完后应关闭连接池 {@link PooledDBC#close()} ，以释放持有的连接对象资源</p>
 * <p>该池通过 {@link #getConn()} 提供的连接对象使用完后应显式关闭（或通过try-with-resource),
 * 以此将连接对象归还到连接池中</p>
 * <pre>
 *     {@code
 *     PooledDBC pooled = DBCFactory.build(...).assertConnect().pooled();
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
 * @see #close()
 * @since 2024/7/25 v0.0.7
 */
public interface PooledDBC extends PooledDB, DBC, Closeable {

    /**
     * 断言能连接到数据库（即能够构建 {@link Connection} 并使用）
     * <p>即给定的参数能连接到数据库，也即表示给定的参数正确，否则抛出异常
     *
     * @return this
     * @throws DBConnectException 当尝试连接数据源失败
     * @see #testConnect()
     */
    @Override
    PooledDBC assertConnect() throws DBConnectException;

    /**
     * 从连接池中获取一个可用的连接对象
     * <p>若没有空闲可用连接对象，且连接对象数量已达到最大值，
     * 则调用该方法的线程将阻塞直到任意一个连接可用</p>
     *
     * @return 连接对象
     * @throws SQLException 当创建或返回{@link Connection} 过程发生异常时
     */
    @Override
    Connection getConn() throws SQLException;
}
