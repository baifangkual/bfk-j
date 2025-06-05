package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.MetaProvider;
import io.github.baifangkual.jlib.db.trait.SchemaDomainMetaProvider;
import io.github.baifangkual.jlib.db.util.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.util.ResultSetConverter;
import io.github.baifangkual.jlib.db.util.SqlSlices;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/7/12
 */
public class PostgresqlDataSource extends SimpleJDBCUrlSliceSynthesizeDataSource {

    private static final MetaProvider META_PROVIDER = new MetaProviderImpl();
    /*
    psql 并未像mysql那样提供查询table和
    */
    // todo dep 这个设定并没有什么卵用?
    private static final String PROP_SCHEMA_KEY = "currentSchema";

    public PostgresqlDataSource(Cfg connConfig) {
        super(connConfig);
    }

    @Override
    protected void preCheckCfg(Cfg cfg) {
        /*
        用户或可将Schema信息放置在other中，这里将改变Config,
        目前的逻辑是一旦在other中设定currentSchema，则复写Config中直接的Schema设置
         */
        cfg.tryGet(DBCCfgOptions.JDBC_PARAMS_OTHER)
                .filter(o -> o.containsKey(PROP_SCHEMA_KEY))
                .map(o -> o.get(PROP_SCHEMA_KEY))
                .ifPresent(schema -> cfg.resetIfNotNull(DBCCfgOptions.SCHEMA, schema));
    }

    @Override
    protected void throwOnIllegalCfg(Cfg cfg) throws IllegalDBCCfgException {
        /* 因为DataSource设定在表的上一级，所以Config中必须要有Schema信息 */
        cfg.tryGet(DBCCfgOptions.SCHEMA)
                .filter(schema -> !schema.isBlank())
                .orElseThrow(() -> new IllegalDBCCfgException("psql not found schema"));
    }

    @Override
    public MetaProvider metaProvider() {
        return META_PROVIDER;
    }

    public static class MetaProviderImpl implements SchemaDomainMetaProvider {


        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, String schema,
                                           Map<String, String> other) throws Exception {
            return DefaultMetaSupport.tablesMeta(conn, db, schema);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String schema,
                                                  String table, Map<String, String> other) throws Exception {
            return DefaultMetaSupport.simpleColumnsMeta(conn, db, schema, table);
        }

        private static final String SELECT_TABLE_TEMPLATE = "SELECT * FROM {} LIMIT {} OFFSET {}";

        @Override
        public Table.Rows tableData(Connection conn, String db, String schema, String table,
                                    Map<String, String> other, Long pageNo, Long pageSize) throws Exception {
            String sql = Stf.f(SELECT_TABLE_TEMPLATE,
                    SqlSlices.safeAdd(db, null, table, SqlSlices.DS_MASK),
                    pageSize, (pageNo - 1) * pageSize);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql);) {
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
