package io.github.baifangkual.jlib.db.utils;

import io.github.baifangkual.jlib.core.func.Fn2;
import io.github.baifangkual.jlib.core.lang.Tup2;
import io.github.baifangkual.jlib.db.entities.Table;
import io.github.baifangkual.jlib.db.exception.ResultSetMappingFailException;
import io.github.baifangkual.jlib.db.exception.ResultSetRowMappingFailException;
import io.github.baifangkual.jlib.db.function.ResultSetMapping;
import io.github.baifangkual.jlib.db.function.ResultSetRowMapping;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * @author baifangkual
 * create time 2024/7/11
 * static class 提供方法，将resultSet值转为多个行对象
 */
public class ResultSetConverter {
    private ResultSetConverter() {
        throw new AssertionError("utility class");
    }

    /**
     * 调用方给定{@link ResultSet}和操作ResultSet中行的函数{@link ResultSetRowMapping},返回多行数据，
     * 该方法不负责调用{@link ResultSet#close()}
     *
     * @param rs         JDBC QUERY查询结果对象,该对象有状态
     * @param rowMapping 描述对{@link ResultSet}中行的操作，因为描述的为对行的操作，
     *                   遂该函数内不应显式调用{@link ResultSet#next()}，除非你有特殊需求（比如跳行读取）
     * @param <ROW>      通过{@link ResultSetRowMapping}函数操作行和返回的对应行的结果
     * @return list[ROW...]
     */
    public static <ROW> List<ROW> rows(ResultSet rs,
                                       ResultSetRowMapping<? extends ROW> rowMapping) {
        try {
            List<ROW> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(rowMapping.map(rs));
            }
            return rows;
        } catch (Exception e) {
            throw new ResultSetRowMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 给定操作整个{@link ResultSet}的函数{@link ResultSetMapping}和{@link ResultSet},返回通过函数转换而来的结果对象，该方法
     * 不同于{@link #rows(ResultSet, ResultSetRowMapping)}方法，要求给定的函数为操作整个{@link ResultSet}的函数，遂要获取多行数据，应
     * 显式在函数体中调用{@link ResultSet#next()}
     *
     * @param resultSetMapping 操作整个{@link ResultSet}的函数
     * @param rs               JDBC QUERY 结果对象
     * @param <ROWS>           表示通过{@link ResultSetMapping}函数操作后返回的结果类型
     * @return ROWS OBJ
     */
    public static <ROWS> ROWS rows(ResultSetMapping<? extends ROWS> resultSetMapping,
                                   ResultSet rs) {
        try {
            return resultSetMapping.map(rs);
        } catch (Exception e) {
            throw new ResultSetMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 与{@link #rows(ResultSet, ResultSetRowMapping)} 相似，不过该函数为{@link Fn2}，即能够操作{@link ResultSet}的某一行，并且
     * 第二个参数也形容了结果集中一行的列的个数
     *
     * @param rs         JDBC QUERY 结果集
     * @param rowMapping 函数，描述对{@link ResultSet}中行的操作
     * @param <ROW>      经过函数操作{@link ResultSet}行后返回的结果对象
     * @return 多行数据 list[ROW...]
     * @deprecated 该方法似乎不常用且方法较为难以理解，后续应要废弃该
     */
    @Deprecated /* 目前就一个引用该方法，该情况是否较少？ */
    public static <ROW> List<ROW> rows(ResultSet rs,
                                       Fn2<? super ResultSet, ? super Integer, ? extends ROW> rowMapping) {
        try {
            int colNum = rs.getMetaData().getColumnCount();
            List<ROW> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(rowMapping.unsafeApply(rs, colNum));
            }
            return rows;
        } catch (Exception e) {
            throw new ResultSetRowMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 给定ResultSet，直接返回读取ResultSet的结果，多行，一行表现为一个{@link Object}数组
     *
     * @param rs JDBC QUERY 结果集
     * @return [Object...]
     */
    public static List<Object[]> rows(ResultSet rs) {
        return rows(rs, (r, cn) -> {
            Object[] row = new Object[cn];
            for (int i = 1; i <= cn; i++) {
                row[i - 1] = rs.getObject(i);
            }
            return row;
        });
    }

    /**
     * 该方法返回{@link #rows(ResultSet)}并且携带表头，通过{@link Tup2}包装，尚不常用，仅在测试使用，后续或删除该
     *
     * @param rs JDBC QUERY 结果集
     * @return (tableHeader, tableRows)
     * @deprecated 后续或删除，因目前仅在测试使用
     */
    @Deprecated /* 废弃，使用 rows 方法 不获取表头？ */
    public static Tup2<List<String>, List<Object[]>> metaAndRows(ResultSet rs) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            List<String> colNames = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                String name = md.getColumnName(i + 1);
                colNames.add(name);
            }
            return Tup2.of(colNames, rows(rs));
        } catch (Exception e) {
            throw new ResultSetRowMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 该方法设定要返回结果集{@link ResultSet}的元数据，通过JDBC API的定义的方法使用，遂正确性依赖于各个数据库支持的正确性，
     * 该方法目前尚不完善，不应使用，后续或补充或废弃
     *
     * @param meta {@link ResultSet#getMetaData()}
     * @return ResultSet元数据
     * @throws Exception 当过程发生异常
     * @deprecated 该未有测试且没时间写，后续或补充或废弃，当前不应使用
     */
    @Deprecated
    private static List<Table.ColumnMeta> meta(ResultSetMetaData meta) throws Exception {
        int colCount = meta.getColumnCount();
        List<Table.ColumnMeta> columnMetas = new ArrayList<>(colCount);
        for (int i = 1; i <= colCount; i++) {
            String colName = meta.getColumnName(i);
            String colTypeName = meta.getColumnTypeName(i);
            String colLabel = meta.getColumnLabel(i); // sql as value
            Table.ColumnMeta columnMeta = new Table.ColumnMeta()
                    .setName(colName)
                    .setTypeName(colTypeName)
                    .setComment(colLabel);
            columnMetas.add(columnMeta);
        }
        return columnMetas;
    }

}
