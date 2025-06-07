package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.IllegalDBCCfgException;
import io.github.baifangkual.jlib.db.impl.abs.DefaultJdbcUrlPaddingDBC;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.trait.NoSchemaJustDBMetaProvider;
import io.github.baifangkual.jlib.db.util.DefaultMetaSupports;
import io.github.baifangkual.jlib.db.util.ResultSetc;
import io.github.baifangkual.jlib.db.util.SqlSlices;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * mysql dbc impl
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public class MysqlDBC extends DefaultJdbcUrlPaddingDBC {

    private static final MetaProvider META_PROVIDER = new MetaProviderImpl();
    private static final int DEFAULT_MYSQL_PORT = 3306;

    public MysqlDBC(Cfg cfg) {
        super(cfg);
    }

    @Override
    protected void preCheckCfg(Cfg cfg) {
        cfg.setIfNotSet(DBCCfgOptions.port, DEFAULT_MYSQL_PORT);
    }

    @Override
    protected void throwOnIllegalCfg(Cfg cfg) throws IllegalDBCCfgException {
        if (cfg.tryGet(DBCCfgOptions.user).isEmpty()) {
            throw new IllegalDBCCfgException("mysql username is empty");
        } else if (cfg.tryGet(DBCCfgOptions.passwd).isEmpty()) {
            throw new IllegalDBCCfgException("mysql password is empty");
        }
    }

    @Override
    public MetaProvider metaProvider() {
        return META_PROVIDER;
    }


    public static class MetaProviderImpl implements NoSchemaJustDBMetaProvider {


        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, Map<String, String> other) throws Exception {
            return DefaultMetaSupports.tablesMeta(conn, db, null);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String table,
                                                  Map<String, String> other) throws Exception {
            return DefaultMetaSupports.simpleColumnsMeta(conn, db, null, table);
        }

        private static final String SELECT_TABLE_TEMPLATE = "SELECT * FROM {} LIMIT {} OFFSET {}";

        @Override
        public Table.Rows tableData(Connection conn,
                                    String db, String table,
                                    Map<String, String> other,
                                    Long pageNo, Long pageSize) throws SQLException {
            // limit 为 要多少行，即pageSize，offset 为 pageNo
            String sql = Stf.f(SELECT_TABLE_TEMPLATE,
                    SqlSlices.safeAdd(db, null, table, SqlSlices.D_MASK),
                    pageSize, (pageNo - 1) * pageSize);
            //noinspection SqlSourceToSinkFlow
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                List<Object[]> rL = ResultSetc.rows(rs);
                return new Table.Rows().setRows(rL);
            }
        }

    }

}
