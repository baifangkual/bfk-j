package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.lang.Tup2;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import lombok.NonNull;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/10/24
 */
public
interface SchemaDomainMetaProvider extends MetaProvider {

    List<Table.Meta> tablesMeta(Connection conn, String db, String schema,
                                Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn,
                                       String db, String schema,
                                       String table,
                                       Map<String, String> other) throws Exception;

    Table.Rows tableData(Connection conn, String db, String schema, String table,
                         Map<String, String> other,
                         Long pageNo, Long pageSize) throws Exception;

    private Tup2<String, String> checkDbAndSchema(Cfg config) {
        final String db = config.tryGet(ConnConfOptions.DB)
                .filter(d -> !d.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(DB_NULL_ERR_MSG));
        final String schema = config.tryGet(ConnConfOptions.SCHEMA)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(SCHEMA_NULL_ERR_MSG));
        return Tup2.of(db, schema);
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        Tup2<String, String> t2 = checkDbAndSchema(config);
        final Map<String, String> o = config.getOrDefault(ConnConfOptions.JDBC_PARAMS_OTHER);
        try {
            return tablesMeta(conn, t2.l(), t2.r(), o);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default Table.Rows tableData(Connection conn, Cfg config,
                                 @NonNull String table,
                                 @NonNull Long pageNo,
                                 @NonNull Long pageSize) {

        Tup2<String, String> t2 = checkDbAndSchema(config);
        Err.realIf(pageNo < 1, IllegalArgumentException::new, "分页参数页码不应小于1");
        final Map<String, String> other = config.getOrDefault(ConnConfOptions.JDBC_PARAMS_OTHER);
        try {
            return tableData(conn, t2.l(), t2.r(), table, other, pageNo, pageSize);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, @NonNull String table) {
        Tup2<String, String> t2 = checkDbAndSchema(config);
        final Map<String, String> other = config.getOrDefault(ConnConfOptions.JDBC_PARAMS_OTHER);
        try {
            return columnsMeta(conn, t2.l(), t2.r(), table, other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }


}
