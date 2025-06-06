package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;

import lombok.NonNull;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/7/15
 * 没有 schema那一级的逻辑
 */
public interface DatabaseDomainMetaProvider extends MetaProvider {


    List<Table.Meta> tablesMeta(Connection conn, String db, Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String table,
                                       Map<String, String> other) throws Exception;

    Table.Rows tableData(Connection conn, String db,
                         String table,
                         Map<String, String> other,
                         Long pageNo, Long pageSize) throws Exception;


    private String checkDb(Cfg config) {
        return config.tryGet(DBCCfgOptions.DB)
                .filter(d -> !d.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(DB_NULL_ERR_MSG));
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        final Map<String, String> other = config.getOrDefault(DBCCfgOptions.JDBC_PARAMS_OTHER);
        try {
            return tablesMeta(conn, checkDb(config), other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default Table.Rows tableData(Connection conn, Cfg config, @NonNull String table,
                                 @NonNull Long pageNo, @NonNull Long pageSize) {
        Err.realIf(pageNo < 1, IllegalArgumentException::new, "分页参数页码不应小于1");
        final Map<String, String> other = config.getOrDefault(DBCCfgOptions.JDBC_PARAMS_OTHER);
        try {
            return tableData(conn, checkDb(config), table, other, pageNo, pageSize);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table) {
        final Map<String, String> other = config.getOrDefault(DBCCfgOptions.JDBC_PARAMS_OTHER);
        try {
            return columnsMeta(conn, checkDb(config), table, other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }


}
