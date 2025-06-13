package io.github.baifangkual.jlib.db.util;

import io.github.baifangkual.jlib.db.Table;
import io.github.baifangkual.jlib.db.RSRowMapping;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;

/**
 * 部分数据库的Jdbc默认行为，公共逻辑，或者说简化
 *
 * @author baifangkual
 * @since 2024/7/16
 */
public class DefaultJdbcMetaSupports {
    private DefaultJdbcMetaSupports() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * jdbc API中表示表所在catalog所在列的列名
     */
    public static final String DEF_T_DB_COL_NAME = "TABLE_CAT";
    /**
     * jdbc API中表示表所在schema所在列的列名
     */
    @SuppressWarnings("SpellCheckingInspection")
    public static final String DEF_T_SCHEMA_COL_NAME = "TABLE_SCHEM";
    /**
     * jdbc API中表示普通表所属类型的所在列的列名称
     */
    public static final String[] DEF_TABLE_TYPES = new String[]{"TABLE"};
    /**
     * jdbc API中表示表名的所在列的列名
     */
    public static final String DEF_COL_TABLE_NAME = "TABLE_NAME";
    /**
     * jdbc API中表示该行描述的所在列的列名，通常表示表注释/列注释
     */
    public static final String DEF_COL_COMMENT = "REMARKS";
    /**
     * jdbc API中表示列名称的所在列的列名
     */
    public static final String DEF_COL_COL_NAME = "COLUMN_NAME";
    /**
     * jdbc API中表示列类型名称的所在列的列名
     */
    public static final String DEF_COL_COL_TYPE = "TYPE_NAME";
    /**
     * jdbc API中表示列的类型的JDBC TYPE CODE的所在列的列名
     */
    public static final String DEF_COL_TYPE_JDBC_CODE = "DATA_TYPE";

    /**
     * jdbc API中表示列的类型的长度的所在列的列名
     */
    public static final String DEF_COL_TYPE_LENGTH = "COLUMN_SIZE";

    /**
     * jdbc API中表示列是否为可空的所在列的列名 (boolean)
     */
    public static final String DEF_COL_IS_NULLABLE = "IS_NULLABLE";

    /**
     * jdbc API中表示列是否可为空所在的列的列名 (int 1|0)
     */
    public static final String DEF_COL_NULLABLE = "NULLABLE";
    /**
     * jdbc API 表示列是否为自增的所在列的列名
     */
    public static final String DEF_COL_IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
    public static final String YES = "YES"; // IS_AUTOINCREMENT 值
    public static final String NO = "NO"; // IS_AUTOINCREMENT 值

    /**
     * jdbc API表示列类型精度的所在列的列名称
     */
    public static final String DEF_COL_DECIMAL_DIGITS = "DECIMAL_DIGITS";


    /**
     * @see #tablesMeta(Connection, String, String, String[], String, String, String, String)
     */
    public static List<Table.Meta> tablesMeta(Connection conn,
                                              String db,
                                              String schema) throws Exception {
        return tablesMeta(conn, db, schema, DEF_COL_TABLE_NAME, DEF_COL_COMMENT);
    }

    /**
     * @see #tablesMeta(Connection, String, String, String[], String, String, String, String)
     */
    public static List<Table.Meta> tablesMeta(Connection conn,
                                              String db,
                                              String schema,
                                              String tableNameColName,
                                              String tableCommentColName) throws Exception {
        return tablesMeta(conn, db, schema, DEF_TABLE_TYPES, tableNameColName, tableCommentColName);
    }

    /**
     * @see #tablesMeta(Connection, String, String, String[], String, String, String, String)
     */
    public static List<Table.Meta> tablesMeta(Connection conn,
                                              String db,
                                              String schema,
                                              String[] tableTypes,
                                              String tableNameColName,
                                              String tableCommentColName) throws Exception {
        return tablesMeta(conn, db, schema, tableTypes, tableNameColName, tableCommentColName, DEF_T_DB_COL_NAME, DEF_T_SCHEMA_COL_NAME);
    }

    /**
     * 使用给定的连接对象，根据给定参数，返回多个表的元数据,
     * 该方法需注意，因{@link Table.Meta}对象无法表示表类型，遂TableTypes未筛选的结果，无法表示类型，
     * 若需表示类型，应拓展该方法返回值可表示类型
     *
     * @param conn                连接对象
     * @param db                  数据库名称
     * @param schema              nullable，部分数据库没有
     * @param tableTypes          索要的表的类型
     * @param tableNameColName    表名所在的列的名称
     * @param tableCommentColName 表注释所在的列的名称
     * @param dbColName           表所在的数据库的数据库名称所在列的列的名称
     * @param schemaColName       表所在的模式所在列的列的名称
     * @return 多个表的元数据
     * @throws Exception 当给定参数使逻辑运行异常时
     */
    public static List<Table.Meta> tablesMeta(Connection conn,
                                              String db,
                                              String schema,
                                              String[] tableTypes,
                                              String tableNameColName,
                                              String tableCommentColName,
                                              String dbColName,
                                              String schemaColName) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet tables = metaData.getTables(db, schema, null, tableTypes);) {
            return ResultSetc.rows(tables,
                    (i, rs) -> new Table.Meta(
                            rs.getString(tableNameColName),
                            rs.getString(tableCommentColName),
                            rs.getString(schemaColName),
                            rs.getString(dbColName)
                    )
            );
        }
    }

    /**
     * @param conn   连接对象
     * @param db     数据库名
     * @param schema 数据库模式名称（部分数据库有该）
     * @param table  表名称
     * @return [ColMeta...]
     * @throws Exception 当给定参数使逻辑运行异常时
     * @see #simpleColumnsMeta(Connection, String, String, String, String, String, String)
     */
    public static List<Table.ColumnMeta> simpleColumnsMeta(Connection conn,
                                                           String db,
                                                           String schema,
                                                           String table) throws Exception {
        return simpleColumnsMeta(conn, db, schema, table, DEF_COL_COL_NAME, DEF_COL_COL_TYPE, DEF_COL_COMMENT);
    }

    /**
     * 给定conn连接对象，给定各项参数，返回某表的列的元数据，仅提供基础元数据，
     * 复杂转换请用{@link ResultSetc#rows(ResultSet, RSRowMapping)}
     *
     * @param conn               连接对象
     * @param db                 数据库名
     * @param schema             数据库模式名称（部分数据库有该）
     * @param table              表名称
     * @param colNameColName     结果集中表示列名称的列
     * @param colTypeNameColName 结果集中表示列类型的列
     * @param colCommentColName  结果集中表示列说明的列
     * @return [ColMeta...]
     * @throws Exception 当给定参数使逻辑运行异常时
     */
    public static List<Table.ColumnMeta> simpleColumnsMeta(Connection conn,
                                                           String db,
                                                           String schema,
                                                           String table,
                                                           String colNameColName,
                                                           String colTypeNameColName,
                                                           String colCommentColName) throws Exception {
        return simpleColumnsMeta(conn, db, schema, table, colNameColName, colTypeNameColName, colCommentColName,
                DEF_COL_TYPE_JDBC_CODE, DEF_COL_TYPE_LENGTH, DEF_COL_NULLABLE, DEF_COL_IS_AUTOINCREMENT, DEF_COL_DECIMAL_DIGITS);
    }

    /**
     * 给定conn连接对象，给定各项参数，返回某表的列的元数据，仅提供基础元数据，
     * 复杂转换请用{@link ResultSetc#rows(ResultSet, RSRowMapping)}
     *
     * @param conn                          连接对象
     * @param db                            数据库名（部分数据库有该）
     * @param schema                        数据库模式名称（部分数据库有该）
     * @param table                         表名称
     * @param colNameColName                结果集中表示列名称的列
     * @param colTypeNameColName            结果集中表示列类型的列
     * @param colCommentColName             结果集中表示列说明的列
     * @param colTypeCodeColName            结果集中表示列类型（JDBC API TYPE CODE）的列
     * @param colLengthColName              结果集中表示列长度的列
     * @param colNullableFlagColName        结果集中表示列可空性的列
     * @param colIsAutoincrementFlagColName 结果集中表示列是否自增的列
     * @return [ColMeta...]
     * @throws Exception 当给定参数使逻辑运行异常时
     */
    public static List<Table.ColumnMeta> simpleColumnsMeta(Connection conn,
                                                           String db,
                                                           String schema,
                                                           String table,
                                                           String colNameColName,
                                                           String colTypeNameColName,
                                                           String colCommentColName,
                                                           String colTypeCodeColName,
                                                           String colLengthColName,
                                                           String colNullableFlagColName,
                                                           String colIsAutoincrementFlagColName,
                                                           String colDecimalDigitsColName) throws Exception {
        return simpleColumnsMeta(conn, db, schema, table,
                (i, rs) -> new Table.ColumnMeta(
                        rs.getString(colNameColName),
                        rs.getString(colTypeNameColName),
                        rs.getInt(colTypeCodeColName),
                        rs.getInt(colLengthColName),
                        rs.getBoolean(colNullableFlagColName),
                        rs.getInt(colDecimalDigitsColName),
                        YES.equals(rs.getString(colIsAutoincrementFlagColName)),
                        rs.getString(colCommentColName)
                )
        );
    }

    /**
     * 给定conn连接对象，给定各项参数，返回某表的列的元数据，仅提供基础元数据，
     * 复杂转换请用{@link ResultSetc#rows(ResultSet, RSRowMapping)}
     *
     * @param conn     连接对象
     * @param db       数据库名
     * @param schema   数据库模式名称（部分数据库有该）
     * @param table    表名称
     * @param fnRowMap 函数，入参是一个状态不断变换(rs.next())的(index, ResultSet)，出参则为行对象ROW
     * @param <ROW>    要转为的行对象的类型
     * @return [ROW...]
     * @throws Exception 当给定参数使逻辑运行异常时
     */
    public static <ROW> List<ROW> simpleColumnsMeta(Connection conn,
                                                    String db,
                                                    String schema,
                                                    String table,
                                                    RSRowMapping<? extends ROW> fnRowMap) throws Exception {
        Objects.requireNonNull(table, "given table is null");
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet colMeta = metaData.getColumns(db, schema, table, null);) {
            return ResultSetc.rows(colMeta, fnRowMap);
        }
    }


}
