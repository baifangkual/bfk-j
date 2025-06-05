package io.github.baifangkual.jlib.db.trait;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.core.lang.Tup2;
import io.github.baifangkual.jlib.core.util.Stf;
import io.github.baifangkual.jlib.db.constants.ConnConfOptions;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;
import io.github.baifangkual.jlib.db.exception.IllegalConnectionConfigException;
import io.github.baifangkual.jlib.db.impl.abs.SimpleJDBCUrlSliceSynthesizeDataSource;
import io.github.baifangkual.jlib.db.trait.DatabaseDomainMetaProvider;
import io.github.baifangkual.jlib.db.trait.MetaProvider;
import io.github.baifangkual.jlib.db.utils.DefaultMetaSupport;
import io.github.baifangkual.jlib.db.utils.ResultSetConverter;
import io.github.baifangkual.jlib.db.utils.SqlSlices;

import static io.github.baifangkual.jlib.db.utils.DefaultMetaSupport.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * create time 2024/10/24
 * <p>
 * 描述只有schema而没有库 或不能通过在一个jdbc url中访问其他库的 类型的数据库的元数据提供者
 */
public interface JustSchemaDomainMetaProvider extends MetaProvider {


    List<Table.Meta> tablesMeta(Connection conn, String schema,
                                Map<String, String> other) throws Exception;

    List<Table.ColumnMeta> columnsMeta(Connection conn, String schema, String table,
                                       Map<String, String> other) throws Exception;

    Table.Rows tableData(Connection conn, String schema, String table,
                         Map<String, String> other,
                         Long pageNo, Long pageSize) throws Exception;

    private Tup2<String, Map<String, String>> unsafeGetSchemaAndOther(Cfg conf) {
        Map<String, String> other = conf.getOrDefault(ConnConfOptions.JDBC_PARAMS_OTHER);
        String schema = conf.tryGet( ConnConfOptions.SCHEMA)
                .orElseThrow(() -> new IllegalArgumentException("给定数据库参数的SCHEMA为空"));
        return Tup2.of(schema, other);
    }

    @Override
    default List<Table.Meta> tablesMeta(Connection conn, Cfg config) {
        Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
        try {
            return this.tablesMeta(conn, t2.l(), t2.r());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table) {
        Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
        try {
            return this.columnsMeta(conn, t2.l(), table, t2.r());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    default Table.Rows tableData(Connection conn, Cfg config, String table, Long pageNo, Long pageSize) {
        if (pageNo == null || pageSize == null) {
            throw new IllegalArgumentException("分页参数不得为空");
        }
        if (pageNo < 1 || pageSize < 1) {
            throw new IllegalArgumentException("pageNo、pageSize不得小于1");
        }
        Tup2<String, Map<String, String>> t2 = unsafeGetSchemaAndOther(config);
        try {
            return this.tableData(conn, t2.l(), table, t2.r(), pageNo, pageSize);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    @Override
    void delTable(DataSource dataSource, String tb);

    @Override
    default void delTable(Connection conn, String db, String tb) {
        // 请更换上层接口暴露的参数，上层参数并不能兼容该 JustSchemaDomainMetaProvider 实现，遂
        // 这里的逻辑暂时搁置，并等待上层接口逻辑变更
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
