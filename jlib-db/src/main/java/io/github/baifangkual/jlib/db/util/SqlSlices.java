package io.github.baifangkual.jlib.db.util;

import java.util.StringJoiner;

/**
 * sql语句残片的操作工具
 *
 * @author baifangkual
 * @apiNote 该工具类内方法不会进行各项参数非空性等校验，
 * 校验均应由外界调用者进行
 * @since 2024/7/15
 */
public class SqlSlices {
    private SqlSlices() {
        throw new UnsupportedOperationException("utility class");
    }

    public static final String P_MASK = ".";
    public static final String D_MASK = "`";
    public static final String DS_MASK = "\"";

    /**
     * 给定数据库名称，模式名称，表名称，合成: 数据库名称.模式名称.表名称<br>
     * <pre>
     *     {@code
     *     String sqlSlice = safeAdd("db", null, "tbName", "`");
     *     Assert.eq("`db`.`tbName`", sqlSlice);
     *     }
     * </pre>
     *
     * @param db     nullable 数据库名称
     * @param schema nullable 模式名称
     * @param table  notnull 表名称
     * @param wrapC  包裹当中每个元素的符号，不同数据库有不同
     * @return str sql残片
     */
    public static String safeAdd(String db, String schema, String table, String wrapC) {
        StringJoiner sj = new StringJoiner(P_MASK);
        if (db != null) {
            sj.add(wrapLR(db, wrapC));
        }
        if (schema != null) {
            sj.add(wrapLR(schema, wrapC));
        }
        return sj.add(wrapLR(table, wrapC)).toString();
    }

    /**
     * 将给定字符串左右添加字符串或字符
     *
     * @param nameRef 给定字符串
     * @param wrapC   包裹给定字符串的左右两边添加的字符串
     * @return "wrapC" + "nameRef" + "wrapC"
     */
    public static String wrapLR(String nameRef, String wrapC) {
        return wrapC + nameRef + wrapC;
    }

}
