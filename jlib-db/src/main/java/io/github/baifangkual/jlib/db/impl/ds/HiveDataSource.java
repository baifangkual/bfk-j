package io.github.baifangkual.jlib.db.impl.ds;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
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


/**
 * @author baifangkual
 * create time 2024/7/23
 */
public class HiveDataSource extends SimpleJDBCUrlSliceSynthesizeDataSource {

    private static final MetaProvider META_PROVIDER = new MetaProviderImpl();

    public HiveDataSource(Cfg connConfig) {
        super(connConfig);
    }

    @Override
    protected void preCheckConfig(Cfg config) {
        /*
        从hive提供的show database角度说，hive没有schema概念，
        但从其实现的jdbc API角度说，hive提供了schema但没有database概念，
        该处逻辑认可第一种，将外侧指定配置中schema给定db配置
         */
        config.tryGet(ConnConfOptions.SCHEMA)
                .ifPresent(db -> config
                        .reset(ConnConfOptions.DB, db)
                        .remove(ConnConfOptions.SCHEMA));
    }

    @Override
    protected void throwOnConnConfigIllegal(Cfg config) throws IllegalConnectionConfigException {

    }

    @Override
    public MetaProvider getMetaProvider() {
        return META_PROVIDER;
    }


    public static class MetaProviderImpl implements DatabaseDomainMetaProvider {

        @Override
        public List<Table.Meta> tablesMeta(Connection conn, String db, Map<String, String> other) throws Exception {
            /*
            hive 用schema替换db
             */
            List<Table.Meta> metas = DefaultMetaSupport.tablesMeta(conn, null, db,
                    DefaultMetaSupport.DEF_TABLE_TYPES,
                    DefaultMetaSupport.DEF_COL_TABLE_NAME,
                    DEF_COL_COMMENT,
                    /*
                    hive jdbc API impl
                    TABLE_CAT, TABLE_SCHEM, TABLE_NAME, TABLE_TYPE, REMARKS
                    ""       , "XX",      , "XX"      , "XX"      , "XX"
                    返回结果中，hive的schema设定为db，hive的schema设定为null
                     */
                    DefaultMetaSupport.DEF_T_SCHEMA_COL_NAME,
                    DefaultMetaSupport.DEF_T_DB_COL_NAME);
            metas.forEach(m -> m.setSchema(null));
            return metas;
        }

        private static final String HIVE_DEF_COL_IS_AUTOINCREMENT = "IS_AUTO_INCREMENT";
        private static final String HIVE_IS_AUTOINCREMENT_NO = "NO";
        private static final String HIVE_IS_AUTOINCREMENT_YES = "YES";

        @Override
        public List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String table, Map<String, String> other) throws Exception {
            return DefaultMetaSupport.simpleColumnsMeta(conn, null, db, table,
                    DEF_COL_COL_NAME, DEF_COL_COL_TYPE, DEF_COL_COMMENT,
                    DEF_COL_TYPE_JDBC_CODE, DEF_COL_TYPE_LENGTH, DEF_COL_NULLABLE, HIVE_DEF_COL_IS_AUTOINCREMENT, DEF_COL_DECIMAL_DIGITS);
        }

        private static final String SQL_Q = "SELECT * FROM {} LIMIT {} OFFSET {}";

        @Override
        public Table.Rows tableData(Connection conn, String db, String table,
                                    Map<String, String> other, Long pageNo, Long pageSize) throws Exception {
            // tod o 委托至mysql?
            String sql = Stf.f(SQL_Q, SqlSlices.safeAdd(db, null, table, SqlSlices.D_MASK),
                    pageSize, (pageNo - 1) * pageSize);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                List<Object[]> rL = ResultSetConverter.rows(rs);
                return new Table.Rows().setRows(rL);
            }
        }

        @Override
        public void delTable(Connection conn, String db, String tb) throws Exception {
            String sql = Stf.f(DEL_TAB_SQL, db, tb);
            try (Statement statement = conn.createStatement()){
                statement.execute(sql);
            }
        }
    }
}
