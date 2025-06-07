package io.github.baifangkual.jlib.db.func;

import io.github.baifangkual.jlib.core.func.Fn;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 函数式接口，该函数表示对给定的一个{@link ResultSet}的完整的操作（不包括关闭ResultSet操作），
 * 该函数实现要求能够将给定的ResulSet转为表示一个多行或单行的结果对象，该函数因表示对ResultSet的完整操作，
 * 遂应当在获取多行时显示调用{@link ResultSet#next()}
 *
 * @author baifangkual
 * @since 2024/7/25
 */

@FunctionalInterface
public interface FnResultSetMapping<ROWS> extends Fn<ResultSet, ROWS> {

    ROWS map(ResultSet rs) throws Exception;

    @Override
    default ROWS unsafeApply(ResultSet resultSet) throws Exception {
        return map(resultSet);
    }


    static <ROW> FnResultSetMapping<List<ROW>> fnListRowsByRsRowMapping(FnResultSetRowMapping<ROW> fnResultSetRowMapping) {
        return (rs) -> {
            List<ROW> list = new ArrayList<>();
            while (rs.next()) {
                list.add(fnResultSetRowMapping.map(rs));
            }
            return list;
        };
    }

}
