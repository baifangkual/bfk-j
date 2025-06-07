package io.github.baifangkual.jlib.db.func;

import io.github.baifangkual.jlib.core.func.Fn;

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
public interface FnResultSetCollector<ROWS> extends Fn<ResultSet, ROWS> {

    ROWS collectRs2Rows(ResultSet rs) throws Exception;

    @Override
    default ROWS unsafeApply(ResultSet resultSet) throws Exception {
        return collectRs2Rows(resultSet);
    }


    static <ROW> FnResultSetCollector<List<ROW>> fnListRowsCollectByRsRowMapping(FnRSRowMapping<? extends ROW> fnRSRowMapping,
                                                                                 Supplier<? extends List<ROW>> listFactory) {
        return (rs) -> {
            List<ROW> list = listFactory.get();
            int index = 0;
            while (rs.next()) {
                list.add(fnRSRowMapping.collectOneRow(index++, rs));
            }
            return list;
        };
    }

}
