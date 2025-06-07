package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.func.FnRSRowMapping;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;
import io.github.baifangkual.jlib.db.util.ResultSetc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.function.Supplier;

/**
 * <b>数据库连接器（Database Connector）</b>
 * <p>不可变对象，该对象被创建出来后，便可以获取数据源的 {@link Connection} 对象
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
public interface DBC {

    /**
     * 连接的数据库类型
     *
     * @return 数据库类型
     */
    DBType type();

    /**
     * 检查连接是否可用
     *
     * @return true为可用，反之不可用
     * @see #assertConn()
     */
    default boolean testConn() {
        try {
            assertConn();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 断言能连接到数据库（即能够构建 {@link Connection} 并使用）
     * <p>即给定的参数能连接到数据库，也即表示给定的参数正确，否则抛出异常
     *
     * @throws Exception 当尝试连接数据源失败
     * @see #testConn()
     */
    void assertConn() throws Exception;

    /**
     * 获取一个连接对象
     * <p>不负责conn对象的管理和关闭等，conn 使用结束之后的关闭应由调用方负责
     *
     * @return {@link Connection}
     * @throws Exception 当创建conn对象过程中发生异常
     */
    Connection getConn() throws Exception;

    /**
     * 尝试获取一个连接对象，获取失败时返回 {@link R.Err} 携带异常（获取失败原因）
     *
     * @return {@code R.Ok(Connection)} | {@code R.Err(Exception)}
     */
    default R<Connection> tryGetConn() {
        return R.ofFnCallable(this::getConn);
    }

    /**
     * 执行sql查询并返回结果
     * <p>返回的结果将根据给定的 {@link FnRSRowMapping} 函数进行转换,
     * 与 {@link #execQuery(String, FnResultSetCollector)} 不同，要求的函数不应负责 {@link ResultSet#next()} 的调用，
     * 也因此，该函数 {@link FnRSRowMapping} 应仅从 {@link ResultSet} 中获取某一行的数据并返回 {@link ROW} 即可，
     * 方法内部会自动调用 {@code Result.next()}
     * <p>该方法不会检查sql注入问题
     * <pre>{@code
     * record Person(int id, String name, int age) { }
     * DBC dbc = DBCFactory.build(...);
     * List<Indexed<Person>> indexedPerson = dbc.execQuery(
     *         "select id, name, age from person",
     *         ArrayList::new,
     *         // no need call rs.next(),
     *         // or you want to skip some row
     *         (rowOnRsIndex, rs) -> Indexed.of(
     *                 rowOnRsIndex, // just on rs index, not person id
     *                 new Person(rs.getInt("id"),
     *                         rs.getString("name"),
     *                         rs.getInt("age")
     *                 )
     *         )
     * );
     * }</pre>
     *
     * @param <ROW>       ResultSet中行转换为的行对象
     * @param sql         要执行的sql查询语句
     * @param listFactory 函数-提供一个List，形如 {@code ArrayList::new}
     * @param fnRowMap    函数-ResultSet中行的转换方法，函数入参为 {@code (int index, ResultSet rs)}，
     *                    其中 {@code int index} 表示当前rs中的行索引（从0开始），
     *                    该函数不应负责 {@link ResultSet#next()} 的调用
     * @return ResultSet中多个行, 构成了List[ROW...]
     * @see FnRSRowMapping
     * @see #execQuery(String, FnResultSetCollector)
     */
    default <ROW> List<ROW> execQuery(String sql, Supplier<? extends List<ROW>> listFactory,
                                      FnRSRowMapping<? extends ROW> fnRowMap) {
        return this.execQuery(sql, FnResultSetCollector.fnListRowsCollectByRsRowMapping(fnRowMap, listFactory));
    }

    /**
     * 执行sql查询并返回结果
     * <p>返回的结果将根据给定的 {@link FnResultSetCollector} 函数进行转换，该函数 {@link FnResultSetCollector}
     * 拥有 {@link ResultSet} 的完全控制权力，遂该函数内应负责显示调用 {@link ResultSet#next()} 方法，函数将 {@link ResultSet} 完全
     * 转为 {@link ROWS} 类型对象，该结果对象或可包含多行数据
     * <p>该方法不会检查sql注入问题
     * <pre>{@code
     * record Person(int id, String name, int age) { }
     * DBC dbc = DBCFactory.build(...);
     * List<Person> person10 = dbc.execQuery(
     *         "select id, name, age from person",
     *         (rs) -> {
     *             List<Person> personFirst10 = new ArrayList<>();
     *             int count = 0;
     *             // use FnResultSetCollector should call rs.next()
     *             // or you just want read one row
     *             while (count++ < 10 && rs.next()) {
     *                 Person p = new Person(rs.getInt("id"),
     *                         rs.getString("name"),
     *                         rs.getInt("age"));
     *                 personFirst10.add(p);
     *             }
     *             return personFirst10;
     *         }
     * );
     * }</pre>
     *
     * @param <ROWS>  返回值，查询结果
     * @param sql     要执行的sql查询语句
     * @param fnRsMap 函数-入参为ResultSet，返回值为 {@link ROWS} ,该函数或应负责 {@link ResultSet#next()} 的调用
     * @return 查询结果对象
     * @see FnResultSetCollector
     * @see #execQuery(String, Supplier, FnRSRowMapping)
     */
    default <ROWS> ROWS execQuery(String sql, FnResultSetCollector<? extends ROWS> fnRsMap) {
        //noinspection SqlSourceToSinkFlow
        try (Connection conn = getConn();
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(sql)) {
            return ResultSetc.rows(fnRsMap, rs);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    /**
     * 以当前数据库连接器为蓝本，创建一个连接池
     * <p>一个数据库连接器可创建多个连接池，创建的连接池彼此互不影响</p>
     *
     * @param maxPoolSize 最大连接数
     * @return 连接池
     * @see #pooled()
     */
    PooledDBC pooled(int maxPoolSize);

    /**
     * 以当前数据库连接器为蓝本，创建一个连接池
     * <p>一个数据库连接器可创建多个连接池，创建的连接池彼此互不影响</p>
     * <p>该方法将使用构造该数据库连接器时的Cfg配置 {@link DBCCfgOptions#maxPoolSize}，
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
     * @apiNote 这些元信息是通过Jdbc标准Api {@link java.sql.ResultSetMetaData} 获取的
     */
    List<Table.ColumnMeta> columnsMeta(String table);

    /**
     * 给定表名，查询表中符合分页要求的行
     *
     * @param table    表名
     * @param pageNo   页码-分页参数-最小值为1
     * @param pageSize 页大小-分页参数-最小值为1
     * @param fnRsMap  函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @param <ROWS>   表示表中数据对象的类型
     * @return 表中符合分页要求的行
     */
    <ROWS> ROWS tableData(String table, int pageNo, int pageSize,
                          FnResultSetCollector<? extends ROWS> fnRsMap);

    /**
     * 给定表名，查询表中所有行
     *
     * @param table   表名
     * @param fnRsMap 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @return 表中所有行
     */
    <ROWS> ROWS tableData(String table,
                          FnResultSetCollector<? extends ROWS> fnRsMap);


}
