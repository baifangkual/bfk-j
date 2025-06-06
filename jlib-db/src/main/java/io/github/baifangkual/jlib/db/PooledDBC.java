package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.db.func.RsMapping;
import io.github.baifangkual.jlib.db.func.RsRowMapping;
import io.github.baifangkual.jlib.db.impl.pool.ConnectionPoolDBC;

import java.sql.Connection;

/**
 * <b>池化的DBC</b>
 * <p>线程安全，能够管理自己生产的 {@link Connection} 对象生命周期，
 * 相对于仅能提供Connection对象的不可变的 {@link DBC} 类型，该类型在生命周期内内部状态将会有变化<br>
 * 该类型的构造过程应如 {@link DBC} 类型一样，将构造行为委托至 {@link DBCFactory}<br>
 * @implNote 当前该类型无连接保活等行为措施，遂当前的连接保活依赖于各数据库JDBC驱动的实现，若JDBC驱动实现中无连接保活
 * 相关策略，则该无连接保活行为
 * <p>该类型的构造应委托：
 * <pre>
 *     {@code
 *     Config config = new ConnectionConfig()
 *                 .set...
 *                 .setDsType(DSType...)
 *                 .toConfig();
 *     CloseableDataSource connPool = DataSourceCreators.createConnPool(config, 5);
 *     }
 * </pre>
 * 该类型的使用：
 * <pre>
 *     {@code
 *     // 外侧显式获取连接对象并操作，最后应显式调用close或使用try-with-resource语法糖关闭
 *     CompletableFuture.runAsync(() -> {
 *             try (Connection c = connPool.getConnection()){
 *                 // do some...
 *             } catch (Exception e) {
 *                 throw new RuntimeException(e);
 *             }
 *     });
 *     // 或可直接委托操作至该类型，需给定操作 java.sql.ResultSet 类型的函数
 *     List<Table.Meta> metas = connPool.execQuery("select * from table",
 *                 rs -> new Table.Meta()
 *                         .setName(rs.getString("name"))
 *                         .setSchema(rs.getString("schema"))
 *                         .setComment(rs.getString("comment")));
 *     }
 * </pre>
 *
 * @author baifangkual
 * @see DBC#execQuery(String, RsRowMapping)
 * @see DBC#execQuery(RsMapping, String)
 * @see #close()
 * @see RsMapping
 * @see RsRowMapping
 * @since 2024/7/25
 */
public interface PooledDBC extends DBC, Closeable {

    /**
     * 获取一个连接对象，不同的{@link PooledDBC}的该方法的行为应该是不同的
     *
     * @return 一个连接对象，实现方应确保该类型的正确性
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

    @Override
    boolean isClosed();

}
