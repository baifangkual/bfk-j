package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.lang.R;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.func.FnResultSetMapping;
import io.github.baifangkual.jlib.db.func.FnResultSetRowMapping;
import io.github.baifangkual.jlib.db.util.ResultSetc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * todo doc rewrite ....................
 *
 * @author baifangkual
 * create time 2024/7/11
 * <b>数据库连接器（Database Connector）</b>
 * <p>
 * 原本想使用java API的{@link javax.sql.DataSource} 接口，但该接口需要 complie作用域依赖数据源driver实现，
 * 为不耦合驱动器driver，遂使用该类型抽象<br>
 * 数据源实体抽象, 当被创建出来时，应当是不可变对象，该对象被创建出来后，便可以获取一个数据源的Connection对象<br>
 * 所有底层DataSource（关于数据库的）仅描述从{@link Cfg} 对象构建{@link Connection}对象过程，
 * 即该底层实现不负责存储和管理{@link Connection}，仅负责提供,遂也不会持有被自己创建的连接对象的引用，{@link Connection}的管理在他者实现,
 * 若需使用能够管理自己创建的数据源的{@link DBC},应使用{@link PooledDBC}<br>
 * 需明确该类型提供的get方法语义和create方法语义不同，create语义已被删除，目前尚未有create语义<br>
 * 该类型构造方式:
 * <pre>
 *     <b>其一</b>
 *     {@code
 *         ConnectionConfig config = new ConnectionConfig()
 *              .set...
 *              .setDsType(DSType.MYSQL);
 *         DataSource dataSource = DataSourceCreators.create(config);
 *     }
 *     <b>其二</b>
 *     {@code
 *          Config config = Config.of()
 *              .set(ConnConfigOptions.DS_TYPE, DSType.MYSQL)
 *              .reset(...)
 *              .setIf(....);
 *          DataSource datasource = DataSourceCreators.create(config);
 *     }
 *     <b>其三，如果你使用datasource-manager-api-rpc-starter包访问数据源管理服务</b>
 *     {@code
 *          DataSourceInfo dsInfo = DSMClient.query("datasourceid");
 *          DataSource ds = dsInfo.toRuntimeDataSource();
 *          // 或可手动之
 *          Config config = dsInfo.toRuntimeConfig();
 *          DataSource dataSource = DataSourceCreators.create(config);
 *     }
 * </pre>
 */
public interface DBC {


    DBType type();

    /**
     * 检查连接是否可用，与{@link #assertConn()} 相比，该方法在检查失败时将返回{@link Boolean#FALSE}
     *
     * @return 布尔值, true为连接可用，false为连接不可用
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
     * 检查该类型数据源的连接是否正常，也即给定的参数能否连接到一个数据源，在数据源服务运行（或数据文件存在）的情况下，
     * 也即表示给定的参数是否正确，当检查不通过，该方法将抛出异常，
     * 若希望返回boolean类型表示连接与否，可使用{@link #testConn()}
     *
     * @throws Exception 当尝试连接数据源失败
     */
    void assertConn() throws Exception;

    /**
     * 该类型{@link DBC}的该方法实现将创建一个新的Connection对象，该不负责conn对象的管理和关闭等，
     * conn使用结束之后的关闭应由调用方负责
     *
     * @return {@link Connection}
     * @throws Exception 当创建conn对象过程中发生异常
     */
    Connection getConn() throws Exception;

    /**
     * 尝试获取一个连接对象，获取失败时返回{@link R.Err}载荷异常（获取失败原因）
     *
     * @return {@code R.Ok(Connection)} | {@code R.Err(Exception)}
     */
    default R<Connection> tryGetConn() {
        return R.ofFnCallable(this::getConn);
    }

    /**
     * 执行sql查询并返回结果，返回的结果将根据给定的{@link FnResultSetRowMapping}函数进行转换,
     * 与{@link #execQuery(FnResultSetMapping, String)}不同，要求的函数不应负责{@link ResultSet#next()}的调用，
     * 也因此，该函数{@link FnResultSetRowMapping}应仅从{@link ResultSet}中获取某一行的数据并返回{@link ROW}即可
     *
     * @param sql      要执行的sql查询语句
     * @param fnRowMap ResultSet中行的转换方法，该函数不应负责{@link ResultSet#next()}的调用
     * @param <ROW>    ResultSet中行转换为的行对象
     * @return ResultSet中多个行, 构成了List[ROW...]
     * @see FnResultSetRowMapping
     */
    default <ROW> List<ROW> execQuery(String sql, FnResultSetRowMapping<? extends ROW> fnRowMap) {
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
     * 执行sql查询并返回结果，返回的结果将根据给定的{@link FnResultSetMapping}函数进行转换，该函数{@link FnResultSetMapping}
     * 拥有{@link ResultSet}的完全控制权力，遂该函数内应负责显示调用{@link ResultSet#next()}方法，函数将{@link ResultSet}完全
     * 转为{@link ROWS}类型对象，该结果对象或可包含多行数据
     *
     * @param fnRsMap 入参为ResultSet，返回值为{@link ROWS},该函数或应负责{@link ResultSet#next()}的调用
     * @param sql     要执行的sql查询语句
     * @param <ROWS>  返回值，查询结果
     * @return 查询结果对象
     * @see FnResultSetMapping
     */
    default <ROWS> ROWS execQuery(FnResultSetMapping<? extends ROWS> fnRsMap, String sql) {
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
     */
    List<Table.Meta> tablesMeta();

    List<Table.ColumnMeta> columnsMeta(String table);

    /**
     * 给定表名，查询表中符合分页要求的行
     *
     * @param table    表名
     * @param pageNo   页码-分页参数-最小值为1
     * @param pageSize 页大小-分页参数-最小值为1
     * @return 表中符合分页要求的行
     */
    Table.Rows tableData(String table, long pageNo, long pageSize);

    /**
     * 给定表名，查询表中所有行
     *
     * @param table 表名
     * @return 表中所有行
     * @apiNote 该方法在表过大时可能造成堆内存溢出，若不明确表大小，建议调用 {@link #tableData(String, long, long)}
     */
    default Table.Rows tableData(String table) {
        return tableData(table, 1L, Long.MAX_VALUE);
    }


}
