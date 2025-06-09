package io.github.baifangkual.jlib.db;

import java.sql.ResultSet;

/**
 * 函数式接口
 * <p>表示对 {@link ResultSet} 的一行结果的收集，因为该函数作用在 {@link ResultSet} 的某一行，
 * 遂该函数内不应当显式调用 {@link ResultSet#next()} 和 {@link ResultSet#close()},
 * {@code ResultSet.next()} 和 {@code ResultSet.close()} 的调用应当由函数调用者进行，而非函数提供者
 *
 * @author baifangkual
 * @since 2024/7/15
 */
@FunctionalInterface
public interface RSRowMapping<ROW> {

    /**
     * 给定行号（第一行行号为0）和 Rs对象，表示将Rs对象中的某行转为 {@link ROW}
     *
     * @param rowNum 行号（第一行行号为0）
     * @param rs     rs
     * @return 表示行数据
     * @throws Exception 转换过程发生异常
     * @apiNote 该函数内不应当显式调用 {@link ResultSet#next()} 和 {@link ResultSet#close()}
     */
    ROW map(int rowNum, ResultSet rs) throws Exception;


}
