package io.github.baifangkual.jlib.db;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * <b>数据库-表</b>
 * <p>表达数据库中普通表的各项元数据（数据库的各种自有特性表类型不支持表达（视图...等等））
 * <p>{@link Table.Meta} 表达表的元数据
 * <p>{@link Table.ColumnMeta} 表达某表列的元数据
 */
public class Table {


    /**
     * 表的元数据
     * <p>表名称、表注释、所属schema、所属db，
     * 仅表达普通表（数据库的各种自有特性表类型不支持表达（视图...等等））
     *
     * @param name    表名
     * @param comment 表说明
     * @param schema  表所属schema (部分数据库有)
     * @param db      表所属db (部分数据库有)
     * @apiNote 该实例为通过读取 {@link java.sql.ResultSetMetaData} 得到的实体，
     * 其内容依赖不同的数据库提供商的实现，字段值可能为 {@code null}，
     * 也因如此，遂所有字段都为 nullable 的引用类型
     */
    public record Meta(String name, String comment, String schema, String db) {
    }

    /**
     * 列的元数据
     * <p>列名， 列类型名，列类型code（JDBC API），列类型长度，是否可空，精度，是否自增，列说明...
     *
     * @param name          列名
     * @param typeName      列类型名 (类型名定义取决于数据库提供商）
     * @param typeCode      列类型code-jdbc API
     * @param typeLength    列类型长度 (长度定义取决于数据库提供商）
     * @param nullable      列是否为nullable
     * @param precision     列类型精度 (精度定义取决于数据库提供商）
     * @param autoIncrement 列是否自增
     * @param comment       列说明
     * @apiNote 该实例为通过读取 {@link java.sql.ResultSetMetaData} 得到的实体，
     * 其内容依赖不同的数据库提供商的实现，字段值可能为 {@code null}，
     * 也因如此，遂所有字段都为 nullable 的引用类型
     */
    public record ColumnMeta(String name,
                             String typeName,
                             Integer typeCode,
                             Integer typeLength,
                             Boolean nullable,
                             Integer precision,
                             Boolean autoIncrement,
                             String comment) {
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    @ToString
    @Deprecated(forRemoval = true, since = "0.0.7")
    public static class Rows implements Iterable<Object[]> {
        private List<Object[]> rows;

        public List<List<Object>> toList() {
            return toList(Function.identity());
        }

        public List<List<String>> toDisplayList() {
            return toList(String::valueOf);
        }

        public <T> List<List<T>> toList(Function<Object, ? extends T> fn) {
            Objects.requireNonNull(fn, "fn is null");
            List<List<T>> list = new ArrayList<>();
            for (Object[] row : rows) {
                List<T> listRow = new ArrayList<>();
                for (Object col : row) {
                    listRow.add(fn.apply(col));
                }
                list.add(listRow);
            }
            return list;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<Object[]> iterator() {
            // 该无法转变为 indexed，因为查出来的可能不是表里的从头查，
            // 即这种情况下索引对不上
            Iterator<Object[]> it = rows.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Object[] next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }
    }


}
