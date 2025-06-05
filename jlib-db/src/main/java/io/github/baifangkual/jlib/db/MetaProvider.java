package io.github.baifangkual.jlib.db;

import io.github.baifangkual.jlib.core.conf.Cfg;
import io.github.baifangkual.jlib.db.exception.DBQueryFailException;

import java.sql.Connection;
import java.util.List;

/**
 * @author baifangkual
 * create time 2024/7/12
 * <p>
 * 接收conn对象，根据给定参数返回所需数据源的元数据等<br>
 * 所有类型实现仅描述行为，不应存储状态或可变状态<br>
 * 该不负责关闭从外侧传递的入参{@link Connection}，仅获取部分数据，即该类型当中描述的是获取某种类型数据库的元数据的逻辑<br>
 * 一种类型数据库{@link DBC}默认行为对应一种类型的{@link MetaProvider}<br>
 * 示例：
 * <pre>
 *      {@code
 *          DataSource ds = ...;
 *          MetaProvider mtpvd = ds.getMetaProvider();
 *          // 通过 元数据提供者 获取部分 元数据
 *          List<Table.Meta> tables = mtpvd.tablesMeta();
 *          List<Table.ColumnMeta> testTable = mtpvd.columnsMeta(ds, "test_table");
 *          Table.Rows rows = mtpvd.tableData(ds, "test_table", 1L, 10L);
 *      }
 * </pre>
 * 重复数据库有默认行为，其他数据库应实现别处<br>
 * 该对象应当仅有实现对某db的部分数据的输出要求，该应为无状态对象或不可变对象，应为无参构造，以便{@link DBC}对象可方便共享引用该<br>
 */

public interface MetaProvider {
    /*
     * todo 20240716 该类型内部，nullable行为 table schema db 需要审视，正确性，
     *  默认最上层的Meta default行为，要求Schema为Nullable行为，该表示代表何？
     *  代表下层可能为DatabaseDomainMeta，所以不需要该，所以可以为null，而ScheamDomainMeta则不允许该为null？
     *  或是更宽泛一些？ 最上层的Schema nullable行为表示即使是SchemaDomainMeta，也可部分允许schema为null，当
     *  SchemaDomainMeta允许schema为null则使用默认Schema（比如psql的public）或是表示某库下的所有Schema？
     */
    String DB_NULL_ERR_MSG = "Database is null";
    String SCHEMA_NULL_ERR_MSG = "Schema is null";


    List<Table.Meta> tablesMeta(Connection conn, Cfg config);

    List<Table.ColumnMeta> columnsMeta(Connection conn, Cfg config, String table);

    /* 要求不得返回整表，因为数据量太大，遂应当在外侧就拦截PageNo和PageSize为null情况，下层不必if适配*/
    Table.Rows tableData(Connection conn, Cfg config, String table, Long pageNo, Long pageSize);

    default List<Table.Meta> tablesMeta(DBC dataSource) {
        try (Connection conn = dataSource.getConn()) {
            return tablesMeta(conn, dataSource.cfg());
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    default List<Table.Meta> tablesMeta(Connection conn, String db, String schema) {
        return tablesMeta(conn, Cfg.newCfg()
                .setIfNotNull(DBCCfgOptions.DB, db)
                .setIfNotNull(DBCCfgOptions.SCHEMA, schema));
    }

    default List<Table.ColumnMeta> columnsMeta(DBC dataSource, String table) {
        try (Connection conn = dataSource.getConn()) {
            return columnsMeta(conn, dataSource.cfg(), table);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    default List<Table.ColumnMeta> columnsMeta(Connection conn, String db, String schema, String table) {
        return columnsMeta(conn, Cfg.newCfg()
                .setIfNotNull(DBCCfgOptions.DB, db)
                .setIfNotNull(DBCCfgOptions.SCHEMA, schema), table);
    }

    default Table.Rows tableData(DBC dataSource, String table, Long pageNo, Long pageSize) {
        try (Connection conn = dataSource.getConn()) {
            return tableData(conn, dataSource.cfg(), table, pageNo, pageSize);
        } catch (Exception e) {
            throw new DBQueryFailException(e.getMessage(), e);
        }
    }

    default Table.Rows tableData(Connection conn, String db,
                                 String schema,
                                 String table,
                                 Long pageNo, Long pageSize) {
        return tableData(conn, Cfg.newCfg()
                .setIfNotNull(DBCCfgOptions.DB, db)
                .setIfNotNull(DBCCfgOptions.SCHEMA, schema), table, pageNo, pageSize);
    }


}
