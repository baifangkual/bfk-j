package io.github.baifangkual.jlib.db;

import lombok.EqualsAndHashCode;
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
 * namespace table
 */
public class Table {


    /**
     * 表的元数据，仅表示名称和表注释，注释可能为null，
     * 也因此，仅显示普通表（数据库的各种自有特性表类型不支持（视图...等等））
     */
    @Setter
    @Getter
    @Accessors(chain = true)
    @ToString
    @EqualsAndHashCode
    public static class Meta {
        private String db;
        private String schema;
        private String name;
        private String comment;
    }


    /**
     * 载荷一个列的元数据，列名， 列类型名，列类型code（JDBC API），列类型长度，是否可空，精度，是否自增，列说明...
     */
    @Setter
    @Getter
    @Accessors(chain = true)
    @ToString
    public static class ColumnMeta {
        private String name;
        private String typeName;
        private Integer typeCode;
        private Integer typeLength;
        private Boolean nullable;
        private Integer precision;
        private Boolean autoIncrement;
        private String comment;
    }

    @Setter
    @Getter
    @Accessors(chain = true)
    @ToString
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
