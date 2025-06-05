package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.enums.URLType;
import io.github.baifangkual.jlib.db.exception.DropTableFailException;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.JustSchemaDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author baifangkual
 * create time 2024/10/24
 * <p>
 * oracle 数据库 datasource实现<br>
 * 20241024：当前在oracle 11g版本上进行了测试，其他oracle版本或不支持
 */
public class OracleDataSource extends SimpleJDBCUrlSliceSynthesizeDataSource {


    public OracleDataSource(Cfg connConfig) {
        super(connConfig);
    }

    private static final String S_COLON = ":";
    private static final String S_SLASH = "/";
    private static final String W_THIN = ":thin:@";

    @Override
    protected String buildingJdbcUrl(Cfg config) {
        /*
         * 对两种方式的 支持 sid 和 servername，这两种方式的路径形式不同
         * jdbc:oracle:thin:@localhost:1521:sid
         * jdbc:oracle:thin:@localhost:1521/serviceName
         * ORA-12504, TNS:listener was not given the SERVICE_NAME in CONNECT_DATA
         */
        final String prefix = urlPrefix(config); // jdbc:oracle
        final String db = config.get(ConnConfOptions.DB); // oracle 的 db 不参与 sql，仅连接使用
        final URLType ut = config.get(ConnConfOptions.JDBC_URL_TYPE);
        final String app = switch (ut) {
            case JDBC_ORACLE_SERVICE_NAME -> S_SLASH;
            case JDBC_ORACLE_SID -> S_COLON;
            default -> throw new IllegalConnectionConfigException("不合适的URL类型");
        };
        return new StringBuilder()
                .append(prefix)
                .append(W_THIN)
                .append(config.get(ConnConfOptions.HOST))
                .append(S_COLON)
                .append(config.get(ConnConfOptions.PORT))
                .append(app)
                .append(db).toString();
    }

    private static final int DEFAULT_ORACLE_PORT = 1521;

    @Override
    protected void preCheckConfig(Cfg config) {
        // 当用户未给定 URLType 则 使用 servername形式,
        // 当使用 JDBC_DEFAULT 时则转为 JDBC_ORACLE_SERVICE_NAME
        Optional<URLType> ut = config.tryGet(ConnConfOptions.JDBC_URL_TYPE);
        if (ut.isPresent()) {
            config.resetIf(ut.get() == URLType.JDBC_DEFAULT,
                    ConnConfOptions.JDBC_URL_TYPE, URLType.JDBC_ORACLE_SERVICE_NAME);
        } else {
            config.reset(ConnConfOptions.JDBC_URL_TYPE, URLType.JDBC_ORACLE_SERVICE_NAME);
        }
        // 当用户未给定 PORT 则使用oracle 默认端口 1521
        config.resetIf(config.tryGet(ConnConfOptions.PORT).isEmpty(),
                ConnConfOptions.PORT, DEFAULT_ORACLE_PORT);
    }

    @Override
    protected void throwOnConnConfigIllegal(Cfg config) throws IllegalConnectionConfigException {
        if (config.tryGet(ConnConfOptions.DB).isEmpty()) {
            throw new IllegalConnectionConfigException("未配置服务名或SID");
        }
        if (config.tryGet(ConnConfOptions.USER).isEmpty()) {
            throw new IllegalConnectionConfigException("未配置用户名");
        }
    }

    @Override
    protected void postCheckConfig(Cfg config) {

        /*
         throwOnConnConfigIllegal 在该方法之前已经执行，其会检查user 配置与否，遂该处一定不为null
         经过测试发现连接参数的用户名在使用大写或小写的情况下都可以连接到 oracle, 甚至只需要字母对应上即可
         但对于 schema 的查询，其 schema 必须为 大写，妈的
         另外，经过测试，通过navicat 查询 oracle，页面上 oracle 的 连接下显示许多用户名大写的图标为其他库”schema“的元素，

        -- 场景：当前用户为 admin (或者说是 ADMIN，或者说是 aDmin，都他妈可以，只要是这这几个字母，都可以)
        --   场景补充：通过 SELECT username FROM all_users 查出来的所有用户名，都是大写的，草泥马
        -- 登录密码则严格要求大小写
        -- 当前登录的用户可读写其名为"ADMIN" 的 "schema"，也可读其他名字的"schema"，但似乎没有建表等权限
        -- 似乎通过 SELECT * FROM session_privs; 可以查询当前 会话的权限？

        -- 在 "ADMIN"的"schema"下，测试了下述操作，垃圾数据库：
        -- create table test_no_schema1(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA1 的表
        -- create table Test_no_schema2(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA2 的表
        -- create table TEST_NO_SCHEMA3(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA3 的表
        -- create table ADMIN.TEST_NO_SCHEMA4(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA4 的表
        -- create table ADMIN.test_no_schema5(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA5 的表
        -- create table admin.test_no_schema6(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA6 的表
        -- create table aDmIn.test_no_schema7(name char(10)); 在ADMIN下创建了名称为 TEST_NO_SCHEMA7 的表
        -- create table "test_no_schema7"(name char(10)); 在ADMIN下创建了名称为 test_no_schema7 的表，并且与 TEST_NO_SCHEMA7 共存
        -- create table "tEst_No_schema7"(name char(10)); 在ADMIN下创建了名称为 tEst_No_schema7 的表，并且与 TEST_NO_SCHEMA7、test_no_schema7 共存
        -- create table "ADMIN.TEST_No_schema7"(name char(10)); 在ADMIN下创建了名称为 "ADMIN.TEST_No_schema7" 的表
        -- create table "ADMIN"."TEST_No_schema7"(name char(10)); 在ADMIN下创建了名称为 TEST_No_schema7 的表
        -- create table "admin"."TEST_No_schema7"(name char(10)); 提示 ORA-01918: user 'admin' does not exist
        -- create table 'ADMIN'.'TTTT_No_schEma7'(name char(10)); 提示 ORA-00903: invalid table name，然后使用了别的双引号，
        去掉双引号等试了试，发现都为 invalid table，即明确：oracle中"'"符号不应使用
        遂因为 ”“ 的存在 描述表名 和 schema 名称时，最好都加上该符号
         */
        String lowSchema = config.tryGet(ConnConfOptions.SCHEMA)
                .orElse(config.get(ConnConfOptions.USER));
        // 如果用户未设置 schema ，则将用户名大写，然后转为 schema存储
        // 无论如何 schema 转为 大写
        String upperCaseSchema = lowSchema.toUpperCase();
        config.reset(ConnConfOptions.SCHEMA, upperCaseSchema);

    }

    private static final String CHECK_CONN_SQL = "SELECT 1 FROM DUAL";

    @Override
    public void checkConnection() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(CHECK_CONN_SQL)) {
            while (rs.next()) {
                int i = rs.getInt(1);
                if (i == 1) return;
            }
        }
    }

    private static final OracleMetaProvider META_PROVIDER = new OracleMetaProvider();

    @Override
    public MetaProvider getMetaProvider() {
        return META_PROVIDER;
    }


    public static class OracleMetaProvider implements JustSchemaDomainMetaProvider {

        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String schema,
                                           Map<String, String> other) throws Exception {
            // 经测试，oracle 获取表的元数据 的 行为和 sqlserver类似：都不能获取表的描述
            // TABLE_CAT 始终为 null
            return DefaultMetaSupport.tablesMeta(conn, null, schema);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String schema, String table,
                                                  Map<String, String> other) throws Exception {
            // REMARKS 始终为 null 获取不到表描述
            // TABLE_CAT 始终为 null
            return DefaultMetaSupport.simpleColumnsMeta(conn, null, schema, table);
        }

        /**
         * oracle数据库12c之前的版本没有方便的形如 offset 及 limit等的方法，
         * 仅可通过 ROWNUM 获取行号，并通过between方式筛选的办法实现伪分页，
         * 在12c版本后，添加了 “OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY” 的方式，
         * 但为兼容较早期的版本，如11g，没有使用该方式<br>
         * 另外，发现该查询若以 ”;“ 结尾，则Oracle会提示 ORA-00911: 无效字符,
         * 尚不清楚这是oracle jdbc实现的问题还是jdbc设定问题
         */
        private static final String PAGE_QUERY_TEMPLATE = """
                SELECT * FROM (
                    SELECT e.*, ROWNUM AS "{}" FROM {} e
                ) WHERE "{}" BETWEEN {} AND {}""";
        private static final DateTimeFormatter RANDOM_ROW_COL_NAME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        private static String pageQuery(String rowNumColName, Long pageNo, Long pageSize, String schema, String table) {
            long betweenLeft = ((pageNo - 1) * pageSize) + 1;
            long betweenRight = pageNo * pageSize;
            String fullTableName = SqlSlices.safeAdd(null, schema, table, SqlSlices.DS_MASK);
            return Stf.f(PAGE_QUERY_TEMPLATE, rowNumColName, fullTableName, rowNumColName, betweenLeft, betweenRight);
        }

        /**
         * 将分页查询的结果转为 {@link Table.Rows}对象，
         * 因为oracle 分页查询使用了  {@link #PAGE_QUERY_TEMPLATE} 做模板查询，
         * 遂查询结果的最后一列的名称应当为 给定的随机数拼接的列名称，该列被忽略
         */
        private static Table.Rows handlerPageQueryResultSet(ResultSet rs, String rowNumColName) throws SQLException {
            int columnCount = rs.getMetaData().getColumnCount();
            String colName = rs.getMetaData().getColumnName(columnCount);
            // 最后一列列名总是 rowNumColName，要忽略其
            if (!colName.equals(rowNumColName)) {
                throw new SQLException("最后一列列名称不为 " + rowNumColName + "，而是 " + colName);
            }
            List<Object[]> rows = new LinkedList<>();
            while (rs.next()) {
                // 不取最后一列表示行号的值, 遂Object[] 长度为 colCount - 1
                Object[] row = new Object[columnCount - 1];
                // 不取最后一列表示行号的值, 遂 i 取值为 1 - colCount - 1
                for (int i = 1; i < columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            return new Table.Rows().setRows(rows);
        }

        @Override
        public Table.Rows tableData(Connection conn, String schema, String table,
                                    Map<String, String> other, Long pageNo, Long pageSize) throws Exception {
            // 防止列名重名而造成混淆，遂每次重新生成列名
            String rowNumCol = "row" + LocalDateTime.now().format(RANDOM_ROW_COL_NAME);
            String sql = pageQuery(rowNumCol, pageNo, pageSize, schema, table);
            try (Statement stat = conn.createStatement();
                 ResultSet rs = stat.executeQuery(sql)) {
                return handlerPageQueryResultSet(rs, rowNumCol);
            }
        }


        /**
         * 在oracle中执行类似于drop table if exists的操作，
         * 因为oracle中没有 if exists 表示的方法，在oracle中，删除某表的操作，当表
         * 不存在时，将会发生异常 ORA-00942，
         * 遂这里通过该 PLSQL 方式执行删除表的逻辑：
         * <ul>
         *     <li>EXECUTE IMMEDIATE 'DROP TABLE your_table_name'; 用于动态执行删除指定表的 SQL 语句</li>
         *     <li>WHEN OTHERS THEN 用于捕获所有异常。</li>
         *     <li>IF SQLCODE != -942 THEN 用于检查捕获的异常是否是表不存在的错误代码 -942</li>
         *     <li>如果不是 -942，则重新抛出异常；否则，忽略该异常。</li>
         * </ul>
         */
        private static final String DROP_TABLE_TEMPLATE = """
                BEGIN
                    EXECUTE IMMEDIATE 'DROP TABLE {}';
                EXCEPTION
                    WHEN OTHERS THEN
                        IF SQLCODE != -942 THEN
                            RAISE;
                        END IF;
                END;
                """;

        /**
         * 给定 某个 oracle 数据源，和表名，删除指定的表，该方法确保结果一致性
         */
        @Override
        public void delTable(DataSource dataSource, String tb) {
            String schema = dataSource.getConfig().get(ConnConfOptions.SCHEMA);
            String dropSlice = SqlSlices.safeAdd(null, schema, tb, SqlSlices.DS_MASK);
            String dropTableIfExistsSql = Stf.f(DROP_TABLE_TEMPLATE, dropSlice);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()
            ) {
                // 经测试和查看jdbc该API说明，该接口将始终返回false，
                // 该false表示执行的sql不会产生ResultSet
                // 当其他异常时（非表不存在的异常）则stmt.execute抛出SQLException
                stmt.execute(dropTableIfExistsSql);
            } catch (Exception e) {
                throw new DropTableFailException(e.getMessage(), e);
            }
        }
    }
}
