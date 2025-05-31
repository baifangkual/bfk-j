package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.panic.Err;

import java.math.BigInteger;

/**
 * <b>Radix Converter</b><br>
 * 数字值进制转换
 * <p>支持2-62进制之间的进制转换，
 * 为什么是62？因为0-9A-Za-z共62个，这62个字符均为ASCII可显示字符
 * <p>16进制 (0-F)，36进制 (0-Z)，62进制 (0-z)
 *
 * @author baifangkual
 * @since 2025/5/31 v0.0.7
 */
@SuppressWarnings("SpellCheckingInspection")
public class Radixc {

    private Radixc() {
        throw new IllegalAccessError("util class");
    }

    // 字符集：0-9, a-z, A-Z 共62个字符
    private static final char[] B62NUMBERS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z',
    };
    // 反向对照表
    private static final int[] LOOKUP;


    static {
        // 初始化字符转换表
        int cL = B62NUMBERS.length;
        LOOKUP = new int['z' + 1];
        for (int i = 0; i < cL; i++) {
            LOOKUP[B62NUMBERS[i]] = i;
        }
    }

    /**
     * 计算不同基的多项式项数<br>
     * base已知，系数数组已知，如果要把它转换为另一个base2的多项式，base2 < base，
     * 假设第一个多项式 base = b1 共 n 项，
     * 转换为第二个多项式 base = b2 共 m 项，现在就是要计算 m 的最小值。
     * m 什么时候最小，就是该多项式的和最大的时候，每一项都是 b2 - 1，和为 b2^m - 1，
     * 第一个多项式的和也需要在最大的情况下，才能计算出m的最小值，也就是 b1^n - 1
     * 两个多项式的和相等，
     * 所以 b2^m = b1^n，有：
     * <p>数学推导：
     * <pre>
     *   b₂ᵐ = b₁ⁿ
     *   => m = n * log<sub>b₂</sub>(b₁)
     *   => m = n * (ln b₁ / ln b₂)
     * </pre>
     *
     * <a href="https://randtsui.blog.csdn.net/article/details/116377220">多项式变基</a>
     * <p>一个字节可以看作是一个256进制的单个数... {@code 4 = base2Len(2, 256, 16)}
     * 即 2长度256个信号量的信号应转为4长度16个信号量的信号才不会丢失信息
     *
     * @param base1Len 多项式b1项数 （信号长度）
     * @param b1       多项式b1 (b₁ > 0)(信号量）
     * @param b2       多项式b2 (b₂ > 0, b₂ != 1)(信号量）
     * @return 多项式b2项数 m = n · log<sub>b₂</sub>(b₁) （向上取整的）（信号长度）
     * @throws ArithmeticException b2 ≤ 0 或 b2 == 1
     */
    static int base2len(int base1Len, int b1, int b2) {
        if (b1 <= 0 || b2 <= 0 || b2 == 1) {
            throw new ArithmeticException("必须 b2 > 0 且 b2 != 1");
        }
        return (int) Math.ceil(base1Len * (Math.log(b1) / Math.log(b2)));
    }

    /**
     * 将数字字符串从源进制转换为目标进制
     *
     * @param srcNum   要转换的数字字符串
     * @param srcRadix 源进制 (2-62)
     * @param tgtRadix 目标进制 (2-62)
     * @return 转换后的数字字符串
     * @apiNote 该方法并不类似编解码，因为在有实际二进制值有前导0的数据中，
     * 进制转换过程会丢弃前导0。该方法仅负责纯数字的值的进制转换
     */
    public static String convert(String srcNum, int srcRadix, int tgtRadix) {
        Err.realIf(srcNum == null || srcNum.isBlank(),
                IllegalArgumentException::new, "给定的数字为空，srcNum：'" + srcNum + "'");
        // 验证输入
        validateRadix(srcRadix, "源进制");
        validateRadix(tgtRadix, "目标进制");

        //noinspection DataFlowIssue
        char[] numCharArr = srcNum.toCharArray();
        if (isAllZero(numCharArr)) {
            return "0";
        }

        // 先将源进制字符串转换为十进制BigInteger
        BigInteger decimalValue = toDecimal(srcNum, numCharArr, srcRadix);

        // 将十进制值转换为目标进制字符串
        return fromDecimal(decimalValue, tgtRadix);
    }

    /**
     * 将十进制数值转为目标进制下的数值字符串
     *
     * @param src      数值
     * @param tgtRadix 目标进制
     * @return 目标进制下字符串
     */
    public static String convert(long src, int tgtRadix) {
        return fromDecimal(BigInteger.valueOf(src), tgtRadix);
    }

    /**
     * 将十进制数值转为目标进制下的数值字符串
     *
     * @param src      数值
     * @param tgtRadix 目标进制
     * @return 目标进制下字符串
     */
    public static String convert(int src, int tgtRadix) {
        return fromDecimal(BigInteger.valueOf(src), tgtRadix);
    }


    /**
     * 将任意进制字符串转换为十进制BigInteger
     *
     * @apiNote number and numCharArr eq, String每次toCharArray都会创建新的，
     * 为了优化不创建，遂两个都给
     */
    private static BigInteger toDecimal(String number, char[] numCharArr, int srcRadix) {
        // 对于1-36进制，可以使用内置方法优化性能
        if (srcRadix <= 36) {
            return new BigInteger(number, srcRadix);
        }

        // 对于37-62进制，手动转换
        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(srcRadix);

        for (char c : numCharArr) {
            int digit = LOOKUP[c];
            if (digit >= srcRadix) {
                throw new IllegalArgumentException("字符 '" + c + "' 在 " + srcRadix + " 进制中无效");
            }

            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }

        return value;
    }

    /**
     * 将十进制BigInteger转换为目标进制字符串
     */
    private static String fromDecimal(BigInteger decimalValue, int tgtRadix) {
        // 对于1-36进制，可以使用内置方法优化性能
        if (tgtRadix <= 36) {
            return decimalValue.toString(tgtRadix).toUpperCase();
        }

        // 对于37-62进制，手动转换
        if (decimalValue.equals(BigInteger.ZERO)) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(tgtRadix);
        BigInteger current = decimalValue.abs();

        while (current.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] quotientAndRemainder = current.divideAndRemainder(base);
            // 余数在 1
            int remainder = quotientAndRemainder[1].intValue();
            result.insert(0, B62NUMBERS[remainder]);
            current = quotientAndRemainder[0]; // 除完剩下的接着除
        }

        // 处理负数
        if (decimalValue.signum() < 0) {
            result.insert(0, '-');
        }

        return result.toString();
    }

    /**
     * 验证进制是否在有效范围内
     */
    private static void validateRadix(int radix, String name) {
        if (radix < 2 || radix > 62) {
            throw new IllegalArgumentException(name + " 必须在 2 到 62 之间");
        }
    }

    /**
     * 检查字符串是否表示零值
     */
    private static boolean isAllZero(char[] numberCharArr) {
        for (char c : numberCharArr) {
            if (c != '0') {
                return false;
            }
        }
        return true;
    }


}