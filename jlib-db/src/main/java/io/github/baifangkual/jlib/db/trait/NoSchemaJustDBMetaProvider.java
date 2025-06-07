package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 没有schema只有db概念的数据库类型的元数据提供者
 *
 * @author baifangkual
 * @since 2024/7/15
 */
public interface NoSchemaJustDBMetaProvider extends MetaProvider {


    List<Table.Meta> tablesMeta(Connection conn, String db, Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String table,
                                       Map<String, String> other) throws Exception;

    Table.Rows tableData(Connection conn, String db,
                         String table,
                         Map<String, String> other,
                         Long pageNo, Long pageSize) throws Exception;


    private String getDbFromCfg(Cfg config) {
        return config.get(DBCCfgOptions.db);
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        try {
            final Map<String, String> other = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return tablesMeta(conn, getDbFromCfg(config), other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default Table.Rows tableData(Connection conn, Cfg config, String table,
                                 Long pageNo, Long pageSize) {
        try {
            final Map<String, String> other = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return tableData(conn, getDbFromCfg(config), table, other, pageNo, pageSize);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table) {
        try {
            final Map<String, String> other = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return columnsMeta(conn, getDbFromCfg(config), table, other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }


}
