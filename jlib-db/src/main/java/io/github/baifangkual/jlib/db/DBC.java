package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.db.exception.DBConnectException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * <b>数据库连接器（Database Connector）</b>
 * <p>相较于 {@link DB}，该类型实例提供了数据库的部分元数据</p>
 * <p>该对象被创建出来后，便可以获取数据源的 {@link Connection} 对象
 * <p>不负责存储和管理 {@link Connection}，仅负责提供，遂也不会持有被自己创建的连接对象的引用,
 * 若需使用能够管理创建的 {@link Connection} 的 {@link DBC} ,应使用 {@link PooledDBC}
 * <pre>{@code
 * Cfg psqlCfg = Cfg.newCfg()
 *         .set(DBCCfgOptions.host, "...")
 *         .set(DBCCfgOptions.db, "postgres")
 *         .set(DBCCfgOptions.schema, "public")
 *         .set(DBCCfgOptions.user, "...")
 *         .set(DBCCfgOptions.passwd, "******")
 *         .set(DBCCfgOptions.type, DBType.postgresql);
 * DBC dbc = DBCFactory.build(psqlCfg);
 * }</pre>
 *
 * @author baifangkual
 * @since 2024/7/11 v0.0.7
 */
public interface DBC extends DB {


    /**
     * 断言能连接到数据库（即能够构建 {@link Connection} 并使用）
     * <p>即给定的参数能连接到数据库，也即表示给定的参数正确，否则抛出异常
     *
     * @return this
     * @throws DBConnectException 当尝试连接数据源失败
     * @see #testConnect()
     */
    @Override
    DBC assertConnect() throws DBConnectException;


    /**
     * 创建一个连接池
     * <p>一个数据库连接器可创建多个连接池，创建的连接池彼此互不影响</p>
     *
     * @param maxPoolSize 最大连接数
     * @return 连接池
     * @see #pooled()
     */
    @Override
    PooledDBC pooled(int maxPoolSize);

    /**
     * 创建一个连接池
     * <p>一个数据库连接器可创建多个连接池，创建的连接池彼此互不影响</p>
     * <p>该方法将使用构造该数据库连接器时的Cfg配置 {@link DBCCfgOptions#poolMaxSize}，
     * 若没有该项配置，则使用该项配置的默认值</p>
     *
     * @return 连接池
     * @see #pooled(int)
     */
    PooledDBC pooled();

    /**
     * 获取该数据源下所有表的元信息
     *
     * @return 所有表的元信息
     * @apiNote 这些元信息是通过Jdbc标准Api {@link java.sql.ResultSetMetaData} 获取的，
     * 返回的值的覆盖情况取决于数据库提供商的实现，部分数据库没有提供所有的元信息，
     * (比如 Postgresql 的返回不会提供 {@code TABLE_CAT} 信息)
     */
    List<Table.Meta> tablesMeta();

    /**
     * 获取该数据源系某表的列元信息
     *
     * @param table 表名
     * @return 列元信息
     * @apiNote 这些元信息是通过Jdbc标准Api {@link java.sql.ResultSetMetaData} 获取的，
     * 返回的值的覆盖情况取决于数据库提供商的实现，部分数据库没有提供所有的元信息
     */
    List<Table.ColumnMeta> columnsMeta(String table);

    /**
     * 给定表名，查询表中符合分页要求的行
     *
     * @param table       表名
     * @param pageNo      页码-分页参数-最小值为1
     * @param pageSize    页大小-分页参数-最小值为1
     * @param rsExtractor 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @param <ROWS>      表示表中数据对象的类型
     * @return 表中符合分页要求的行
     * @see ResultSetExtractor#fnListRowsByRsRowMapping(RSRowMapping, Supplier)
     */
    <ROWS> ROWS tableData(String table, int pageNo, int pageSize,
                          ResultSetExtractor<? extends ROWS> rsExtractor);

    /**
     * 给定表名，查询表中所有行
     *
     * @param table       表名
     * @param rsExtractor 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @return 表中所有行
     * @see ResultSetExtractor#fnListRowsByRsRowMapping(RSRowMapping, Supplier)
     */
    <ROWS> ROWS tableData(String table,
                          ResultSetExtractor<? extends ROWS> rsExtractor);


}
