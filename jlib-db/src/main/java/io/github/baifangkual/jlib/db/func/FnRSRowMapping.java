package io.github.baifangkual.jlib.db.func;

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
public interface FnRSRowMapping<ROW> {

    /**
     * 给定索引（第一行索引为0）和 Rs对象，表示将Rs对象中的某行转为 {@link ROW}
     *
     * @param index 索引（第一行索引为0）
     * @param rs    rs
     * @return 表示行数据
     * @throws Exception 转换过程发生异常
     */
    ROW collectOneRow(int index, ResultSet rs) throws Exception;


}
