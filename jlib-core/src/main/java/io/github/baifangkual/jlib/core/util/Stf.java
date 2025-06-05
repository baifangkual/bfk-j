package io.github.baifangkual.jlib.core.util;

import io.github.baifangkual.jlib.core.Const;

import java.util.Objects;

/**
 * <b>string formatter</b>
 * <pre>
 *  {@code
 *  String str = Stf.f("a,b,{},d,e,\\{},\\\\g", "c");
 *  Assert.eq("a,b,c,d,e,\{},\\g", str);
 *  Assert.throwE(NullPointException.class, ()-> Stf.f(null, "c"));
 *  }
 * </pre>
 * 该类主要逻辑参考 hutool-core 包下 StrFormatter
 *
 * @author baifangkual
 * @see #f(String, Object...)
 * @since 2024/6/19 v0.0.3
 */
public final class Stf {
    /**
     * 不允许实例化
     */
    private Stf() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 反斜杠（\)
     */
    private static final char C_BACKSLASH = Const.Char.BACKSLASH;
    /**
     * 花括号（{}）
     */
    private static final String PLACEHOLDER = Const.String.DELIM_EMPTY;
    /**
     * 长度
     */
    private static final int PLACEHOLDER_LENGTH = PLACEHOLDER.length();

    /**
     * 安全使用 nullable 对象的 toString 方法<br>
     * 该方法已经被简化，原方法请参考hutool-core库下 {@code cn.hutool.core.util.StrUtil#utf8Str(java.lang.Object)}
     *
     * @param nullableObj nullable obj
     * @return string or null(Void.class)
     */
    private static String nullableSafeToString(Object nullableObj) {
        if (nullableObj == null) return null;
        // 原本在这方法内应有多个instanceof 判断 但这些场景较少，为了性能考量，该直接使用toString
        return nullableObj.toString();
    }

    /**
     * 格式化字符串<br>
     * 此方法只是简单将指定占位符 按照顺序替换为参数, 如果想输出占位符使用 {@code \\}转义即可，
     * 如果想输出占位符之前的 {@code \} ,使用双转义符 {@code \\\\} 即可<br>
     * <pre>
     *     {@code
     *     Stf.f("this is {} for {}", "a", "b");
     *     // result: this is a for b
     *     Stf.f("this is \\{} for {}", "a", "b");
     *     // result: this is {} for a
     *     Stf.f("this is \\\\{} for {}", "a", "b");
     *     // result: this is \a for b
     *     }
     * </pre>
     *
     * @param temp 字符串模板
     * @param args 参数列表
     * @return format string
     * @throws NullPointerException 给定的字符串模板为空时
     */
    public static String f(String temp, Object... args) {
        Objects.requireNonNull(temp, "string template is null");
        if (temp.isBlank() || args == null || args.length == 0) return temp;
        final int strPatternLength = temp.length();
        // 初始化定义好的长度以获得更好的性能
        final StringBuilder sb = new StringBuilder(strPatternLength + 50);
        int handledPosition = 0;// 记录已经处理到的位置
        int delimIndex;// 占位符所在位置
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            delimIndex = temp.indexOf(PLACEHOLDER, handledPosition);
            if (delimIndex == -1) {// 剩余部分无占位符
                if (handledPosition == 0) { // 不带占位符的模板直接返回
                    return temp;
                }
                // 字符串模板剩余部分不再包含占位符，加入剩余部分后返回结果
                sb.append(temp, handledPosition, strPatternLength);
                return sb.toString();
            }
            // 转义符
            if (delimIndex > 0 && temp.charAt(delimIndex - 1) == C_BACKSLASH) {// 转义符
                if (delimIndex > 1 && temp.charAt(delimIndex - 2) == C_BACKSLASH) {// 双转义符
                    // 转义符之前还有一个转义符，占位符依旧有效
                    sb.append(temp, handledPosition, delimIndex - 1);
                    sb.append(nullableSafeToString(args[argIndex]));
                    handledPosition = delimIndex + PLACEHOLDER_LENGTH;
                } else {
                    // 占位符被转义
                    argIndex--;
                    sb.append(temp, handledPosition, delimIndex - 1);
                    sb.append(PLACEHOLDER.charAt(0));
                    handledPosition = delimIndex + 1;
                }
            } else {// 正常占位符
                sb.append(temp, handledPosition, delimIndex);
                sb.append(nullableSafeToString(args[argIndex]));
                handledPosition = delimIndex + PLACEHOLDER_LENGTH;
            }
        }
        // 加入最后一个占位符后所有的字符
        sb.append(temp, handledPosition, strPatternLength);
        return sb.toString();
    }

}
