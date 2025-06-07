package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.abs.DefaultJdbcUrlPaddingDBC;
import io.github.baifangkual.jlib.db.trait.HasDbAndSchemaMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.util.DefaultMetaSupports;
import io.github.baifangkual.jlib.db.util.ResultSetc;
import io.github.baifangkual.jlib.db.util.SqlSlices;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * psql dbc impl
 *
 * @author baifangkual
 * @since 2024/7/12
 */
public class PostgresqlDBC extends DefaultJdbcUrlPaddingDBC {

    private static final MetaProvider META_PROVIDER = new MetaProviderImpl();

    private static final String PROP_SCHEMA_KEY = "currentSchema";
    private static final int DEFAULT_PORT = 5432;

    public PostgresqlDBC(Cfg cfg) {
        super(cfg);
    }

    @Override
    protected void preCheckCfg(Cfg cfg) {
        /*
        用户或可将Schema信息放置在other中，这里将改变Config,
        目前的逻辑是一旦在other中设定currentSchema，则复写Config中直接的Schema设置
         */
        cfg.tryGet(DBCCfgOptions.jdbcOtherParams)
                .filter(o -> o.containsKey(PROP_SCHEMA_KEY))
                .map(o -> o.get(PROP_SCHEMA_KEY))
                .ifPresent(schema -> cfg.setIfNotSet(DBCCfgOptions.schema, schema));
        cfg.setIfNotSet(DBCCfgOptions.port, DEFAULT_PORT);
    }

    @Override
    protected void throwOnIllegalCfg(Cfg cfg) throws IllegalDBCCfgException {
        /* 因为DataSource设定在表的上一级，所以Config中必须要有Schema信息 */
        cfg.tryGet(DBCCfgOptions.schema)
                .filter(schema -> !schema.isBlank())
                .orElseThrow(() -> new IllegalDBCCfgException("psql not found schema"));
    }

    @Override
    public MetaProvider metaProvider() {
        return META_PROVIDER;
    }

    public static class MetaProviderImpl implements HasDbAndSchemaMetaProvider {


        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, String schema,
                                           Map<String, String> other) throws Exception {
            // pg的jdbc meta 不会返回 TABLE_CAT
            // 即 没有 dbname
            return DefaultMetaSupports.tablesMeta(conn, db, schema);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String schema,
                                                  String table, Map<String, String> other) throws Exception {
            return DefaultMetaSupports.simpleColumnsMeta(conn, db, schema, table);
        }

        private static final String SELECT_TABLE_TEMPLATE = "SELECT * FROM {} LIMIT {} OFFSET {}";

        @Override
        public Table.Rows tableData(Connection conn, String db, String schema, String table,
                                    Map<String, String> other, Long pageNo, Long pageSize) throws Exception {
            String sql = Stf.f(SELECT_TABLE_TEMPLATE,
                    SqlSlices.safeAdd(db, schema, table, SqlSlices.DS_MASK),
                    pageSize, (pageNo - 1) * pageSize);
            //noinspection SqlSourceToSinkFlow
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql);) {
                List<Object[]> rL = ResultSetc.rows(rs);
                return new Table.Rows().setRows(rL);
            }
        }

    }
}
