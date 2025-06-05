package io.github.baifangkual.jlib.db.func;

import io.github.baifangkual.jlib.core.func.Fn;

import java.sql.ResultSet;

/**
 * @author baifangkual
 * create time 2024/7/15
 * <p>
 * 函数式接口，表示对{@link ResultSet}的某一行结果的转换，因为该函数作用在{@link ResultSet}的某一行，
 * 遂该函数内不应当显示调用{@link ResultSet#next()},ResultSet.next()的调用应当由函数使用者进行，而非函数提供者
 */
@FunctionalInterface
public interface ResultSetRowMapping<ROW> extends Fn<ResultSet, ROW> {


    ROW map(ResultSet rs) throws Exception;


    @Override
    default ROW unsafeApply(ResultSet rs) throws Exception {
        return map(rs);
    }


}
