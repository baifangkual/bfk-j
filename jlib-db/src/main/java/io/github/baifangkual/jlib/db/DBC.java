package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.exception.JdbcConnectionFailException;
import io.github.baifangkual.jlib.db.func.RsMapping;
import io.github.baifangkual.jlib.db.func.RsRowMapping;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.util.ResultSetConverter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * @author baifangkual
 * create time 2024/7/11
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
     * 检查该类型数据源的连接是否正常，也即给定的参数能否连接到一个数据源，在数据源服务运行（或数据文件存在）的情况下，
     * 也即表示给定的参数是否正确，当检查不通过，该方法将抛出异常，
     * 若希望返回boolean类型表示连接与否，可使用{@link #tryCheckConnection()}
     *
     * @throws Exception 当尝试连接数据源失败
     */
    void checkConn() throws Exception;

    /**
     * 该类型{@link DBC}的该方法实现将创建一个新的Connection对象，该不负责conn对象的管理和关闭等，
     * conn使用结束之后的关闭应由调用方负责
     *
     * @return {@link Connection}
     * @throws Exception 当创建conn对象过程中发生异常
     */
    Connection getConn() throws Exception;

    /**
     * 执行sql查询并返回结果，返回的结果将根据给定的{@link RsRowMapping}函数进行转换,
     * 与{@link #execQuery(RsMapping, String)}不同，要求的函数不应负责{@link ResultSet#next()}的调用，
     * 也因此，该函数{@link RsRowMapping}应仅从{@link ResultSet}中获取某一行的数据并返回{@link ROW}即可
     *
     * @param sql        要执行的sql查询语句
     * @param rowMapping ResultSet中行的转换方法，该函数不应负责{@link ResultSet#next()}的调用
     * @param <ROW>      ResultSet中行转换为的行对象
     * @return ResultSet中多个行, 构成了List[ROW...]
     * @see RsRowMapping
     */
    default <ROW> List<ROW> execQuery(String sql, RsRowMapping<? extends ROW> rowMapping) {
        try (Connection conn = getConn();
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(sql)) {
            return ResultSetConverter.rows(rs, rowMapping);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    /**
     * 执行sql查询并返回结果，返回的结果将根据给定的{@link RsMapping}函数进行转换，该函数{@link RsMapping}
     * 拥有{@link ResultSet}的完全控制权力，遂该函数内应负责显示调用{@link ResultSet#next()}方法，函数将{@link ResultSet}完全
     * 转为{@link ROWS}类型对象，该结果对象或可包含多行数据
     *
     * @param resultSetMapping 入参为ResultSet，返回值为{@link ROWS},该函数或应负责{@link ResultSet#next()}的调用
     * @param sql              要执行的sql查询语句
     * @param <ROWS>           返回值，查询结果
     * @return 查询结果对象
     * @see RsMapping
     */
    default <ROWS> ROWS execQuery(RsMapping<? extends ROWS> resultSetMapping, String sql) {
        try (Connection conn = getConn();
             Statement stat = conn.createStatement();
             ResultSet rs = stat.executeQuery(sql)) {
            return ResultSetConverter.rows(resultSetMapping, rs);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    /**
     * 检查连接是否可用，与{@link #checkConn()}相比，该方法在检查失败时将抛出运行时异常
     */
    default void throwableCheckConnection() {
        try {
            checkConn();
        } catch (Exception e) {
            throw new JdbcConnectionFailException(e);
        }
    }

    /**
     * 检查连接是否可用，与{@link #throwableCheckConnection()}相比，该方法在检查失败时将返回{@link Boolean#FALSE}
     *
     * @return 布尔值, true为连接可用，false为连接不可用
     */
    default boolean tryCheckConnection() {
        try {
            throwableCheckConnection();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试获取一个连接对象，获取失败时返回{@link Optional#empty()}，该方法将不会抛出异常
     *
     * @return {@link Optional}
     */
    default Optional<Connection> tryGetConnection() {
        try {
            return Optional.ofNullable(getConn());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 获取该数据源的配置信息
     *
     * @return {@link Cfg}
     */
    Cfg readonlyCfg();

    /**
     * 获取该数据源类型数据库元数据提供者
     *
     * @return {@link MetaProvider}
     */
    MetaProvider metaProvider();

    /**
     * 获取该数据源下所有表的元信息
     *
     * @return 所有表的元信息
     */
    default List<Table.Meta> tablesMeta() {
        try (Connection conn = getConn()) {
            MetaProvider metaProvider = metaProvider();
            return metaProvider.tablesMeta(conn, readonlyCfg());
        } catch (Exception e) {
            throw new JdbcConnectionFailException(e.getMessage(), e);
        }
    }


}
