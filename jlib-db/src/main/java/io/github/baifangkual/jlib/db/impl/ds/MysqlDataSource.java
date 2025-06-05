package io.github.baifangkual.jlib.db.impl.ds;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/7/11
 */
public class MysqlDataSource extends SimpleJDBCUrlSliceSynthesizeDataSource {

    private static final MetaProvider META_PROVIDER = new MetaProviderImpl();

    public MysqlDataSource(Cfg connConfig) {
        super(connConfig);
    }

    @Override
    protected void throwOnConnConfigIllegal(Cfg config) throws IllegalConnectionConfigException {
        if (config.tryGet(ConnConfOptions.USER).isEmpty()) {
            throw new IllegalConnectionConfigException("mysql username is empty");
        } else if (config.tryGet(ConnConfOptions.PASSWD).isEmpty()) {
            throw new IllegalConnectionConfigException("mysql password is empty");
        }
    }

    @Override
    public MetaProvider getMetaProvider() {
        return META_PROVIDER;
    }




    public static class MetaProviderImpl implements DatabaseDomainMetaProvider {


        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, Map<String, String> other) throws Exception {
            return DefaultMetaSupport.tablesMeta(conn, db, null);
        }

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String table,
                                                  Map<String, String> other) throws Exception {
            return DefaultMetaSupport.simpleColumnsMeta(conn, db, null, table);
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
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
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
