package io.github.baifangkual.jlib.db.util;

import io.github.baifangkual.jlib.db.exception.ResultSetMappingFailException;
import io.github.baifangkual.jlib.db.exception.ResultSetRowMappingFailException;
import io.github.baifangkual.jlib.db.func.FnRSRowMapping;
import io.github.baifangkual.jlib.db.func.FnResultSetCollector;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Jdbc ResultSet Converter
 * 提供方法，将resultSet值转为多个行对象
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public class ResultSetc {
    private ResultSetc() {
        throw new AssertionError("utility class");
    }

    /**
     * 调用方给定{@link ResultSet}和操作ResultSet中行的函数{@link FnRSRowMapping},返回多行数据，
     * 该方法不负责调用{@link ResultSet#close()}
     *
     * @param rs       JDBC QUERY查询结果对象,该对象有状态
     * @param fnRowMap 函数-描述对{@link ResultSet}中行的操作，因为描述的为对行的操作，
     *                 遂该函数内不应显式调用{@link ResultSet#next()}，除非你有特殊需求（比如跳行读取）
     * @param <ROW>    通过{@link FnRSRowMapping}函数操作行和返回的对应行的结果
     * @return list[ROW...]
     */
    public static <ROW> List<ROW> rows(ResultSet rs,
                                       FnRSRowMapping<? extends ROW> fnRowMap) {
        try {
            Supplier<ArrayList<ROW>> listFactory = ArrayList::new;
            return FnResultSetCollector.fnListRowsCollectByRsRowMapping(fnRowMap, listFactory)
                    .collectRs2Rows(rs);
        } catch (Exception e) {
            throw new ResultSetRowMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 给定操作整个{@link ResultSet}的函数{@link FnResultSetCollector}和{@link ResultSet},返回通过函数转换而来的结果对象，该方法
     * 不同于{@link #rows(ResultSet, FnRSRowMapping)}方法，要求给定的函数为操作整个{@link ResultSet}的函数，遂要获取多行数据，应
     * 显式在函数体中调用{@link ResultSet#next()}
     *
     * @param fnRsMap 函数-完整读取整个 {@link ResultSet}
     * @param rs      JDBC QUERY 结果对象
     * @param <ROWS>  表示通过 {@link FnResultSetCollector} 函数操作后返回的结果类型
     * @return ROWS OBJ
     */
    public static <ROWS> ROWS rows(FnResultSetCollector<? extends ROWS> fnRsMap,
                                   ResultSet rs) {
        try {
            return fnRsMap.collectRs2Rows(rs);
        } catch (Exception e) {
            throw new ResultSetMappingFailException(e.getMessage(), e);
        }
    }

    /**
     * 给定ResultSet，直接返回读取ResultSet的结果，多行，一行表现为一个{@link Object}数组
     *
     * @param rs JDBC QUERY 结果集
     * @return [Object...]
     */
    public static List<Object[]> rows(ResultSet rs) {
        try {
            int colNum = rs.getMetaData().getColumnCount();
            List<Object[]> rows = new LinkedList<>();
            while (rs.next()) {
                Object[] row = new Object[colNum];
                for (int i = 1; i <= colNum; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            throw new ResultSetRowMappingFailException(e.getMessage(), e);
        }
    }

}
