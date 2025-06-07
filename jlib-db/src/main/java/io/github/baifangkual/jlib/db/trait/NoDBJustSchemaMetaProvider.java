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
 * 只有schema而没有db概念的数据库类型的元数据提供者
 *
 * @author baifangkual
 * @since 2024/10/24
 */
public interface NoDBJustSchemaMetaProvider extends MetaProvider {


    List<Table.Meta> tablesMeta(Connection conn, String schema,
                                Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn, String schema, String table,
                                       Map<String, String> other) throws Exception;

    <ROWS> ROWS tableData(Connection conn, String schema, String table,
                          Map<String, String> other,
                          Long pageNo, Long pageSize,
                          FnResultSetCollector<? extends ROWS> fnResultSetCollector) throws Exception;

    private Tup2<String, Map<String, String>> unsafeGetSchemaAndOther(Cfg conf) {
        Map<String, String> other = conf.getOrDefault(DBCCfgOptions.jdbcOtherParams);
        String schema = conf.get(DBCCfgOptions.schema);
        return Tup2.of(schema, other);
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        try {
            Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
            return this.tablesMeta(conn, t2.l(), t2.r());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table) {
        try {
            Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
            return this.columnsMeta(conn, t2.l(), table, t2.r());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default <ROWS> ROWS tableData(Connection conn, Cfg config, String table, Long pageNo, Long pageSize,
                                  FnResultSetCollector<? extends ROWS> fnResultSetCollector) {
        try {
            Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
            return this.tableData(conn, t2.l(), table, t2.r(), pageNo, pageSize, fnResultSetCollector);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }


}
