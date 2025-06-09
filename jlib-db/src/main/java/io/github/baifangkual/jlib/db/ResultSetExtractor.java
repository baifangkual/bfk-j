package io.github.baifangkual.jlib.db;

import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * 函数式接口
 * <p>该函数表示对给定的一个 {@link ResultSet} 的完整的操作（不包括关闭ResultSet操作），
 * 该函数实现要求能够将给定的 {@code ResulSet} 中的行收集到一个 {@link ROWS} 中，
 * 表示一个 n 行的结果对象，该函数因表示对 {@code ResulSet} 的完整操作，
 * 遂应当在获取多行时显示调用 {@link ResultSet#next()}
 *
 * @author baifangkual
 * @since 2024/7/25
 */

@FunctionalInterface
public interface ResultSetExtractor<ROWS> {

    /**
     * 提取 {@link ResultSet} 中数据，返回一个 {@link ROWS} 实体
     * <p>该方法内应完全操控 RS（比如调用 RS.next())，但不应当关闭其，RS 的关闭应由方法外控制
     *
     * @param rs jdbc-resultSet
     * @return ROWS
     * @throws Exception 提取过程发生异常时
     */
    ROWS extract(ResultSet rs) throws Exception;

    /**
     * 给定List构造方法引用和 {@link RSRowMapping}, 返回一个 {@link ResultSetExtractor} 函数
     */
    static <ROW> ResultSetExtractor<List<ROW>> fnListRowsByRsRowMapping(RSRowMapping<? extends ROW> rowMapping,
                                                                        Supplier<? extends List<ROW>> listFactory) {
        return (rs) -> {
            List<ROW> list = listFactory.get();
            int index = 0;
            while (!rs.isClosed() && rs.next()) {
                list.add(rowMapping.map(index++, rs));
            }
            return list;
        };
    }

}
