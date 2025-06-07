package io.github.baifangkual.jlib.db.func;

import io.github.baifangkual.jlib.core.func.Fn;

import java.sql.ResultSet;

/**
 * 函数式接口
 * <p>表示对 {@link ResultSet} 的一行结果的收集，因为该函数作用在 {@link ResultSet} 的某一行，
 * 遂该函数内不应当显示调用 {@link ResultSet#next()}, {@code ResultSet.next()} 的调用应当由函数使用者进行，而非函数提供者
 *
 * @author baifangkual
 * @since 2024/7/15
 */
@FunctionalInterface
public interface FnRSRowCollector<ROW> extends Fn<ResultSet, ROW> {


    ROW collectOneRow(ResultSet rs) throws Exception;


    @Override
    default ROW unsafeApply(ResultSet rs) throws Exception {
        return collectOneRow(rs);
    }


}
