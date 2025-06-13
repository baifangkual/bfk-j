package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Rng;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.ResultSetExtractor;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryException;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.abs.PrefixSupJdbcUrlPaddingDBC;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.trait.NoDBJustSchemaMetaProvider;
import io.github.baifangkual.jlib.db.util.DefaultJdbcMetaSupports;
import io.github.baifangkual.jlib.db.util.ResultSetc;
import io.github.baifangkual.jlib.db.util.SqlSlices;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import static io.github.baifangkual.jlib.db.util.SqlSlices.DS_MASK;

/**
 * oracle dbc impl<br>
 * <p>20241024: 该实现仅在oracle 11g版本上进行了测试，其他oracle版本或不支持
 *
 * @author baifangkual
 * @since 2024/10/24
 */
public class Oracle11gServerNameJdbcUrlDBC extends PrefixSupJdbcUrlPaddingDBC {


    public Oracle11gServerNameJdbcUrlDBC(Cfg cfg) {
        super(cfg);
    }

    @Override
    public FnAssertValidConnect fnAssertValidConnect() {
        return FnAssertValidConnect.ORACLE_SELECT_1;
    }

    private static final String S_COLON = ":";
    private static final String S_SLASH = "/";
    private static final String JDBC_P = "jdbc:oracle:thin:@//";

    @Override
    protected String buildingJdbcUrl(Cfg readonlyCfg) {
        final String db = readonlyCfg.get(DBCCfgOptions.db);
        //noinspection StringBufferReplaceableByString
        return new StringBuilder()
                .append(JDBC_P)
                .append(readonlyCfg.get(DBCCfgOptions.host))
                .append(S_COLON)
                .append(readonlyCfg.get(DBCCfgOptions.port))
                .append(S_SLASH)
                .append(db)
                .toString();
    }

    private static final int DEFAULT_ORACLE_PORT = 1521;

    @Override
    protected void preCheckCfg(Cfg cfg) {
        // 当用户未给定 PORT 则使用oracle 默认端口 1521
        cfg.setIfNotSet(DBCCfgOptions.port, DEFAULT_ORACLE_PORT);
    }

    @Override
    protected void throwOnIllegalCfg(Cfg cfg) throws IllegalDBCCfgException {
        if (cfg.tryGet(DBCCfgOptions.db).isEmpty()) {
            throw new IllegalDBCCfgException("oracle 未配置服务名");
        }
        if (cfg.tryGet(DBCCfgOptions.user).isEmpty()) {
            throw new IllegalDBCCfgException("oracle 未配置用户名");
        }
    }

    @Override
    protected void postCheckCfg(Cfg cfg) {

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
        String lowSchema = cfg.tryGet(DBCCfgOptions.schema)
                .orElse(cfg.get(DBCCfgOptions.user));
        // 如果用户未设置 schema ，则将用户名大写，然后转为 schema存储
        // 无论如何 schema 转为 大写
        String upperCaseSchema = lowSchema.toUpperCase();
        cfg.reset(DBCCfgOptions.schema, upperCaseSchema);

    }

    public static final String SQL_ORACLE_SELECT_1 = "SELECT 1 FROM DUAL";


    /**
     * 因oracle11g分页较为繁琐，遂在不分页的请求中，不使用 {@link io.github.baifangkual.jlib.db.DBC}
     * 中默认的实现（委托到大分页）
     *
     * @implNote 该实现是不好的，因为行为应该被委托至 {@link MetaProvider} 提供，
     * 而非 {@link io.github.baifangkual.jlib.db.DBC}，
     * 但暂定此，后续大更改时或同时迁移到对应的 {@link MetaProvider}中
     */
    @Override
    public <ROWS> ROWS tableData(String table,
                                 ResultSetExtractor<? extends ROWS> rsExtractor) {
        Objects.requireNonNull(table, "given table is null");
        final String fullTableName = SqlSlices.safeAdd(null,
                readonlyCfg().get(DBCCfgOptions.schema), // 已在postCheckCfg转为大写的了
                table, DS_MASK);
        //noinspection SqlSourceToSinkFlow,SqlNoDataSourceInspection
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + fullTableName)) {
            return ResultSetc.rows(rsExtractor, rs);
        } catch (Exception e) {
            throw new DBQueryException(e.getMessage(), e);
        }
    }

    private static final Oracle11gMetaProvider META_PROVIDER = new Oracle11gMetaProvider();

    @Override
    public MetaProvider metaProvider() {
        return META_PROVIDER;
    }


    public static class Oracle11gMetaProvider implements NoDBJustSchemaMetaProvider {

        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String schema,
                                           Map<String, String> other) throws Exception {
            // 经测试，oracle 获取表的元数据 的 行为和 sqlserver类似：都不能获取表的描述
            // TABLE_CAT 始终为 null
            return DefaultJdbcMetaSupports.tablesMeta(conn, null, schema);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String schema, String table,
                                                  Map<String, String> other) throws Exception {
            // REMARKS 始终为 null 获取不到表描述
            // TABLE_CAT 始终为 null
            return DefaultJdbcMetaSupports.simpleColumnsMeta(conn, null, schema, table);
        }

        /**
         * oracle数据库12c之前的版本没有方便的形如 offset 及 limit等的方法，
         * 仅可通过 ROWNUM 获取行号，并通过between方式筛选的办法实现伪分页，
         * 在12c版本后，添加了 “OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY” 的方式，
         * 但为兼容较早期的版本，如11g，没有使用该方式<br>
         * 另外，发现该查询若以 ”;“ 结尾，则Oracle会提示 ORA-00911: 无效字符,
         * 尚不清楚这是oracle jdbc实现的问题还是jdbc设定问题
         */
        @SuppressWarnings("SpellCheckingInspection")
        private static final String PAGE_QUERY_TEMPLATE = """
                SELECT {} FROM (
                    SELECT e.*, ROWNUM AS "{}" FROM {} e
                ) WHERE "{}" BETWEEN {} AND {}""";

        /**
         * 因为oracle 分页查询使用了  {@link #PAGE_QUERY_TEMPLATE} 做模板查询，
         * 遂查询结果的最后一列的名称应当为 给定的随机数拼接的列名称，该列被忽略，
         * 通过 {@link #queryTableFullColName(Connection, String, String)} 找到所有实际的列名，
         * 然后仅 select 实际的列名列，这些列名为防止与关键字装上，
         * 遂左右都包了 {@value SqlSlices#DS_MASK}，按照oracle11g设定，
         * 包裹后将大小写敏感，这可能会影响到 re.getByName...
         */
        private static String pageQuery(List<String> tableFullColName,
                                        String rowNumColName,
                                        int pageNo,
                                        int pageSize,
                                        String schema,
                                        String table) {
            if (pageNo < 1 || pageSize < 1) {
                throw new IllegalArgumentException("pageNo and pageSize must be greater than 1!");
            }
            final StringJoiner sj = new StringJoiner(", ");
            // 左右括起来，防止 select 遇到关键字
            tableFullColName.forEach(cn -> sj.add(SqlSlices.wrapLR(cn, DS_MASK)));
            final String safeFullCols = sj.toString();
            int betweenLeft = ((pageNo - 1) * pageSize) + 1;
            int betweenRight = pageNo * pageSize;
            String fullTableName = SqlSlices.safeAdd(null, schema, table, DS_MASK);
            return Stf.f(PAGE_QUERY_TEMPLATE,
                    safeFullCols,
                    rowNumColName, fullTableName,
                    rowNumColName, betweenLeft, betweenRight);
        }

        /**
         * 查某表中所有列的列名，不关闭给定的Conn
         */
        private List<String> queryTableFullColName(Connection conn,
                                                   String schema,
                                                   String table) throws Exception {
            return this.columnsMeta(conn, schema, table, Map.of())
                    .stream()
                    .map(Table.ColumnMeta::name).toList();
        }


        @Override
        public <ROWS> ROWS tableData(Connection conn, String schema, String table,
                                     Map<String, String> other,
                                     int pageNo, int pageSize,
                                     ResultSetExtractor<? extends ROWS> resultSetExtractor) throws Exception {
            // 防止列名重名而造成混淆，遂每次重新生成列名
            String rngRowNumCol = "row" + Rng.nextFixLenLarge(20);
            List<String> tableFullColName = queryTableFullColName(conn, schema, table);
            String sql = pageQuery(tableFullColName, rngRowNumCol, pageNo, pageSize, schema, table);
            try (Statement stat = conn.createStatement();
                 ResultSet rs = stat.executeQuery(sql)) {
                return ResultSetc.rows(resultSetExtractor, rs);
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
         *
         * @deprecated 删除接口 dropTable，已不支持删除表，
         * 该 oracle11g 删除表过程 sql 暂存于此
         */
        @Deprecated(since = "0.0.7")
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

    }
}
