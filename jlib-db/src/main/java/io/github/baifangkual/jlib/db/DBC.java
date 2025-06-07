package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.func.FnRSRowCollector;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;
import io.github.baifangkual.jlib.db.util.ResultSetc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

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
     * 执行sql查询并返回结果，返回的结果将根据给定的 {@link FnRSRowCollector} 函数进行转换,
     * 与 {@link #execQuery(FnResultSetCollector, String)} 不同，要求的函数不应负责 {@link ResultSet#next()} 的调用，
     * 也因此，该函数 {@link FnRSRowCollector} 应仅从 {@link ResultSet} 中获取某一行的数据并返回 {@link ROW} 即可
     *
     * @param sql      要执行的sql查询语句
     * @param fnRowMap ResultSet中行的转换方法，该函数不应负责 {@link ResultSet#next()} 的调用
     * @param <ROW>    ResultSet中行转换为的行对象
     * @return ResultSet中多个行, 构成了List[ROW...]
     * @see FnRSRowCollector
     * @see #execQuery(FnResultSetCollector, String)
     */
    default <ROW> List<ROW> execQuery(String sql, FnRSRowCollector<? extends ROW> fnRowMap) {
        //noinspection SqlSourceToSinkFlow
        try (Connection conn = getConn();
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(sql)) {
            return ResultSetc.rows(rs, fnRowMap);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    /**
     * 执行sql查询并返回结果，返回的结果将根据给定的 {@link FnResultSetCollector} 函数进行转换，该函数 {@link FnResultSetCollector}
     * 拥有 {@link ResultSet} 的完全控制权力，遂该函数内应负责显示调用 {@link ResultSet#next()} 方法，函数将 {@link ResultSet} 完全
     * 转为 {@link ROWS} 类型对象，该结果对象或可包含多行数据
     *
     * @param fnRsMap 入参为ResultSet，返回值为 {@link ROWS} ,该函数或应负责 {@link ResultSet#next()} 的调用
     * @param sql     要执行的sql查询语句
     * @param <ROWS>  返回值，查询结果
     * @return 查询结果对象
     * @see FnResultSetCollector
     * @see #execQuery(String, FnRSRowCollector)
     */
    default <ROWS> ROWS execQuery(FnResultSetCollector<? extends ROWS> fnRsMap, String sql) {
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
     * @param table                表名
     * @param pageNo               页码-分页参数-最小值为1
     * @param pageSize             页大小-分页参数-最小值为1
     * @param fnResultSetCollector 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @param <ROWS>               表示表中数据对象的类型
     * @return 表中符合分页要求的行
     */
    <ROWS> ROWS tableData(String table, long pageNo, long pageSize,
                          FnResultSetCollector<? extends ROWS> fnResultSetCollector);

    /**
     * 给定表名，查询表中所有行
     *
     * @param table                表名
     * @param fnResultSetCollector 函数-描述完整读取 {@link ResultSet} 并将其中的数据行转为 {@link ROWS} 的过程
     * @return 表中所有行
     * @apiNote 该方法实际是委托至 {@link #tableData(String, long, long, FnResultSetCollector)} 方法并给定分页参数默认值
     * {@code pageNo = 1L, pageSize = Long.MAX_VALUE}，这可能会对数据库造成一定的计算性能消耗，
     * 且可能数据库的分页参数并不支持 Long.MAX_VALUE 这么大，遂该方法后续应重写为简单的表查询即可，而不应该委托至分页查询，
     * 而且 {@code tableData} 系方法参数及返回值已修改，通过 {@link FnResultSetCollector} 函数，外界可自由控制读取行数，
     * 遂对于原本的分页查询实现 {@link #tableData(String, long, long, FnResultSetCollector)}，可能应做到覆盖
     */
    default <ROWS> ROWS tableData(String table,
                                  FnResultSetCollector<? extends ROWS> fnResultSetCollector) {
        return tableData(table, 1L, Long.MAX_VALUE, fnResultSetCollector);
    }


}
