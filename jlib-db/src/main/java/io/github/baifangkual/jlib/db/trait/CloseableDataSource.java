package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.trait.Closeable;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;

import java.sql.Connection;

/**
 * @author baifangkual
 * create time 2024/7/25
 * <p>
 * 可关闭的DataSource，能够管理自己生产的{@link Connection}对象生命周期，
 * 相对于仅能提供Connection对象的不可变的{@link DataSource}类型，该类型在生命周期内内部状态将会有变化<br>
 * 需明确该类型提供的get方法语义和create方法语义不同，create语义已删除，目前尚未有create语义<br>
 * 目前（20240725）的设想不能保证该类型的所有实现都线程安全，详细安全性说明应查看相应实现的说明<br>
 * 所有该类型实现应确保调用方使用完毕该类型后一定调用{@link #close()}方法以释放资源<br>
 * 所有该类型实现应确保：close方法不可重复调用关闭；close方法调用后该类型不应再能够正常使用<br>
 * 该类型的构造过程应提供良好的方式或较为简单的使用方法，或可如{@link DataSource}类型一样，将构造行为委托至
 * {@link com.btrc.datacenter.common.datasource.utils.DataSourceCreators}<br>
 * 20240730追加：当前该有实现类线程安全的连接池实现该，具体可见
 * {@link ConnectionPoolProxyDataSource}<br>
 * 20240924: 当前该类型无连接保活等行为措施，遂当前的连接保活依赖于各数据库JDBC驱动的实现，若JDBC驱动实现中无连接保活
 * 相关策略，则该无连接保活行为，应测试使用该，后续或开发连接保活行为能力<br>
 * <p>
 * 该类型的构造应委托：
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
 * @see DataSource#execQuery(String, ResultSetRowMapping)
 * @see DataSource#execQuery(ResultSetMapping, String)
 * @see #close()
 * @see ResultSetMapping
 * @see ResultSetRowMapping
 */
public interface CloseableDataSource extends DataSource, Closeable {

    /**
     * 获取一个连接对象，不同的{@link CloseableDataSource}的该方法的行为应该是不同的
     *
     * @return 一个连接对象，实现方应确保该类型的正确性
     * @throws Exception 当创建或返回{@link Connection} 过程发生异常时
     */
    @Override
    Connection getConnection() throws Exception;

    /**
     * 该类型使用完毕后，确保一定调用该方法以释放资源
     *
     * @throws Exception 当close过程发生异常
     */
    @Override
    void close() throws Exception;

    @Override
    boolean isClosed();

}
