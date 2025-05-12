package io.github.baifangkual.bfk.j.mod.core.immutable;

/**
 * 常量引用存放地<br>
 * 存放到该类中的常量的命名规则：
 * <ul>
 *     <li>根据其值的实际内容命名，而不应该根据其用途命名，因为一个常量值的用途可能有多个</li>
 *     <li>该类下所定义的常量应根据其类型做划分，类型名称全程都应该为{@code Const.xxx}</li>
 * </ul>
 * 该类中部分常量定义来自于hutool包下相关常量定义<br>
 *
 * @author baifangkual
 * @since 2024/11/15 v0.0.3
 */
public final class Const {

    /**
     * 字符串常量
     */
    public static final class String {
        /**
         * 字符串常量：空json {@code "{}"}
         */
        public static final java.lang.String DELIM_EMPTY = "{}";

        /**
         * 字符串常量：制表符 {@code "\t"}
         */
        public static final java.lang.String TAB = "	";

        /**
         * 字符串常量：点 {@code "."}
         */
        public static final java.lang.String DOT = ".";

        /**
         * 字符串常量：双点 {@code ".."} <br>
         * 用途：作为指向上级文件夹的路径，如：{@code "../path"}
         */
        public static final java.lang.String DOUBLE_DOT = "..";

        /**
         * 字符串常量：斜杠 {@code "/"}
         */
        public static final java.lang.String SLASH = "/";

        /**
         * 字符串常量：反斜杠 {@code "\\"}
         */
        public static final java.lang.String BACKSLASH = "\\";

        /**
         * 字符串常量：回车符 {@code "\r"} <br>
         * 解释：该字符常用于表示 Linux 系统和 MacOS 系统下的文本换行
         */
        public static final java.lang.String CR = "\r";

        /**
         * 字符串常量：换行符 {@code "\n"}
         */
        public static final java.lang.String LF = "\n";

        /**
         * 字符串常量：Windows 换行 {@code "\r\n"} <br>
         * 解释：该字符串常用于表示 Windows 系统下的文本换行
         */
        public static final java.lang.String CRLF = "\r\n";

        /**
         * 字符串常量：下划线 {@code "_"}
         */
        public static final java.lang.String UNDERLINE = "_";

        /**
         * 字符串常量：减号（连接符） {@code "-"}
         */
        public static final java.lang.String DASHED = "-";

        /**
         * 字符串常量：逗号 {@code ","}
         */
        public static final java.lang.String COMMA = ",";

        /**
         * 字符串常量：花括号（左） <code>"{"</code>
         */
        public static final java.lang.String DELIM_START = "{";

        /**
         * 字符串常量：花括号（右） <code>"}"</code>
         */
        public static final java.lang.String DELIM_END = "}";

        /**
         * 字符串常量：中括号（左） {@code "["}
         */
        public static final java.lang.String BRACKET_START = "[";

        /**
         * 字符串常量：中括号（右） {@code "]"}
         */
        public static final java.lang.String BRACKET_END = "]";

        /**
         * 字符串常量：冒号 {@code ":"}
         */
        public static final java.lang.String COLON = ":";

        /**
         * 字符串常量：艾特 {@code "@"}
         */
        public static final java.lang.String AT = "@";


    }

    /**
     * 字符常量
     */
    public static final class Char {
        /**
         * 字符常量：空格符 {@code ' '}
         */
        public static final char SPACE = ' ';
        /**
         * 字符常量：制表符 {@code '\t'}
         */
        public static final char TAB = '	';
        /**
         * 字符常量：点 {@code '.'}
         */
        public static final char DOT = '.';
        /**
         * 字符常量：斜杠 {@code '/'}
         */
        public static final char SLASH = '/';
        /**
         * 字符常量：反斜杠 {@code '\\'}
         */
        public static final char BACKSLASH = '\\';
        /**
         * 字符常量：回车符 {@code '\r'}
         */
        public static final char CR = '\r';
        /**
         * 字符常量：换行符 {@code '\n'}
         */
        public static final char LF = '\n';
        /**
         * 字符常量：减号（连接符） {@code '-'}
         */
        public static final char DASHED = '-';
        /**
         * 字符常量：下划线 {@code '_'}
         */
        public static final char UNDERLINE = '_';
        /**
         * 字符常量：逗号 {@code ','}
         */
        public static final char COMMA = ',';
        /**
         * 字符常量：花括号（左） <code>'{'</code>
         */
        public static final char DELIM_START = '{';
        /**
         * 字符常量：花括号（右） <code>'}'</code>
         */
        public static final char DELIM_END = '}';
        /**
         * 字符常量：中括号（左） {@code '['}
         */
        public static final char BRACKET_START = '[';
        /**
         * 字符常量：中括号（右） {@code ']'}
         */
        public static final char BRACKET_END = ']';
        /**
         * 字符常量：双引号 {@code '"'}
         */
        public static final char DOUBLE_QUOTES = '"';
        /**
         * 字符常量：单引号 {@code '\''}
         */
        public static final char SINGLE_QUOTE = '\'';
        /**
         * 字符常量：与 {@code '&'}
         */
        public static final char AMP = '&';
        /**
         * 字符常量：冒号 {@code ':'}
         */
        public static final char COLON = ':';
        /**
         * 字符常量：艾特 {@code '@'}
         */
        public static final char AT = '@';
    }


}
