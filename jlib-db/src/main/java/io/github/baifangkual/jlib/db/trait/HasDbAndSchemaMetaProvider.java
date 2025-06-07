package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.lang.Tup2;
import io.github.baifangkual.jlib.db.DBCCfgOptions;
import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 有db概念和schema概念的数据库类型的元数据提供者
 *
 * @author baifangkual
 * @since 2024/10/24
 */
public
interface HasDbAndSchemaMetaProvider extends MetaProvider {

    List<Table.Meta> tablesMeta(Connection conn, String db, String schema,
                                Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn,
                                       String db, String schema,
                                       String table,
                                       Map<String, String> other) throws Exception;

    <ROWS> ROWS tableData(Connection conn, String db, String schema, String table,
                          Map<String, String> other,
                          long pageNo, long pageSize,
                          FnResultSetCollector<? extends ROWS> fnResultSetCollector) throws Exception;

    private Tup2<String, String> unsafeGetDbAndSchemaFromCfg(Cfg config) {
        final String db = config.get(DBCCfgOptions.db);
        final String schema = config.get(DBCCfgOptions.schema);
        return Tup2.of(db, schema);
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        try {
            Tup2<String, String> t2 = unsafeGetDbAndSchemaFromCfg(config);
            final Map<String, String> o = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return tablesMeta(conn, t2.l(), t2.r(), o);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default <ROWS> ROWS tableData(Connection conn, Cfg config,
                                  String table,
                                  long pageNo,
                                  long pageSize,
                                  FnResultSetCollector<? extends ROWS> fnResultSetCollector) {
        try {
            Tup2<String, String> t2 = unsafeGetDbAndSchemaFromCfg(config);
            final Map<String, String> other = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return tableData(conn, t2.l(), t2.r(), table, other, pageNo, pageSize, fnResultSetCollector);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table) {
        try {
            Tup2<String, String> t2 = unsafeGetDbAndSchemaFromCfg(config);
            final Map<String, String> other = config.getOrDefault(DBCCfgOptions.jdbcOtherParams);
            return columnsMeta(conn, t2.l(), t2.r(), table, other);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }


}
