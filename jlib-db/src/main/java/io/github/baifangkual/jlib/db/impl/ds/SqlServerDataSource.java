package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.trait.SchemaDomainMetaProvider;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/7/19
 */
public class SqlServerDataSource extends SimpleJDBCUrlSliceSynthesizeDataSource {

    private final static MetaProvider META_PROVIDER = new MeteProviderImpl();
    /*
    https://blog.csdn.net/fengchunhua518/article/details/135699804
    默认应设置：
    "trustServerCertificate", "true"
    "encrypt", "false"
    否则现版本会默认设置encrypt为true并trustServerCertificate为false导致连接异常
     */
    private final static String TRUST_SERVER_CERTIFICATE = "trustServerCertificate";
    private final static String DEFAULT_TRUST_SERVER_CERTIFICATE = "true";
    private final static String ENCRYPT = "encrypt";
    private final static String DEFAULT_ENCRYPT = "false";
    private final static String DB_ON_P = "database";

    public SqlServerDataSource(Cfg connConfig) {
        super(connConfig);
    }

    @Override
    protected void preCheckConfig(Cfg config) {
        Map<String, String> other = config.tryGet(ConnConfOptions.JDBC_PARAMS_OTHER).orElse(Map.of());
        Map<String, String> newO = new HashMap<>(other);
        // trustServerCertificate encrypt...
        if (!newO.containsKey(TRUST_SERVER_CERTIFICATE)) {
            newO.put(TRUST_SERVER_CERTIFICATE, DEFAULT_TRUST_SERVER_CERTIFICATE);
        }
        if (!newO.containsKey(ENCRYPT)) {
            newO.put(ENCRYPT, DEFAULT_ENCRYPT);
        }
        config.reset(ConnConfOptions.JDBC_PARAMS_OTHER, newO);
        // database=... eg:;database=master
        if (newO.containsKey(DB_ON_P)) {
            config.resetIfNotNull(ConnConfOptions.DB, newO.get(DB_ON_P));
        }

    }

    @Override
    protected String buildingJdbcUrl(Cfg config) {
        String url = super.buildingJdbcUrl(config);
        String msUrl = url.substring(0, url.lastIndexOf("/"));
        String dbName = config.get(ConnConfOptions.DB);
        return Stf.f("{};{}={}", msUrl, DB_ON_P, dbName);
    }


    @Override
    protected void throwOnConnConfigIllegal(Cfg config) throws IllegalConnectionConfigException {

    }

    @Override
    public MetaProvider getMetaProvider() {
        return META_PROVIDER;
    }

    public static class MeteProviderImpl implements SchemaDomainMetaProvider {

        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, String schema, Map<String, String> other) throws Exception {
            /*
            https://learn.microsoft.com/zh-cn/sql/connect/jdbc/reference/gettables-method-sqlserverdatabasemetadata?view=sql-server-ver16
            MSSQL JDBC getTables实现中 REMARKS列始终为null，后或可从 INFORMATION_SCHEMA.TABLES 中获取
            */
            return DefaultMetaSupport.tablesMeta(conn, db, schema);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String schema, String table, Map<String, String> other) throws Exception {
            /*
            https://learn.microsoft.com/zh-cn/sql/connect/jdbc/reference/getcolumns-method-sqlserverdatabasemetadata?view=sql-server-ver16
            MSSQL JDBC getColumns实现中 REMARKS列始终为null，后或可从 INFORMATION_SCHEMA.TABLES 中获取
            */
            return DefaultMetaSupport.simpleColumnsMeta(conn, db, schema, table);
        }

        private static final String QUERY_P = "SELECT * FROM {} ORDER BY (SELECT NULL) OFFSET {} ROWS FETCH NEXT {} ROWS ONLY";

        @Override
        public Table.Rows tableData(Connection conn, String db, String schema, String table,
                                    Map<String, String> other,
                                    Long pageNo, Long pageSize) throws Exception {
            String sql = Stf.f(QUERY_P,
                    SqlSlices.safeAdd(db, schema, table, SqlSlices.DS_MASK),
                    (pageNo - 1) * pageSize,
                    pageSize);
            try (Statement stat = conn.createStatement();
                 ResultSet rs = stat.executeQuery(sql)) {
                List<Object[]> rL = ResultSetConverter.rows(rs);
                return new Table.Rows().setRows(rL);
            }
        }

        @Override
        public void delTable(Connection conn, String db, String tb) throws Exception {
            throw new UnsupportedOperationException("Not implemented yet.");
        }
    }
}
