package io.github.baifangkual.jlib.core.codec;

import io.github.baifangkual.jlib.core.util.Stf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <b>BaseN编解码器</b><br>
 * 不可变，线程安全，该编码器将数据以二进制位 M 为单位分为若干块，每块分别以一个ASCII字符值进行编码，
 * 支持任意 N = 2^M 基（M: 1-8）的编解码，基（N）由字母表大小决定<br>
 * 内有 {@link #b16}、{@link #b64}、{@link #b64Url} 编解码器默认实例<br>
 * <p>可通过自定的 {@link BNCodec.Table} 自定编解码器实例行为<br>
 * 该编解码器无法构造一般意义的 {@code base62} 的行为，
 * 因为 base62 的算法并不是该类实现的 分块编码 算法，
 * 可参阅：
 * <a href="https://stackoverflow.com/questions/69832449/are-there-multiple-base62-encoding-algorithm">multiple-base62</a>
 *
 * @author baifangkual
 * @apiNote 当 N 不为 2^M (M: 1-8) 时，强行在该类型上实现 BaseN 是无意义的，
 * 因为其字母表的后 N - 1 - (highBit(N) - 1) 个字符在编码时一定不会被选取到，
 * （比如使用该类型实现base62，则字母表后 30（= 62 - 1 - 31）个字符一定不会被选取到，
 * 这与 base32 无异）<br>
 * @since 2025/5/30 v0.0.7
 */
public final class BNCodec implements Codec<byte[], byte[]> {

    /**
     * 字母表配置
     *
     * @param nTable          字母表字符集 (2-256)
     * @param nullablePadding 剩余位填充字符 (可选)
     */
    public record Table(char[] nTable, Character nullablePadding) {


        public static Table of(char[] chars, Character padding) {
            return new Table(chars, padding);
        }

        public static Table of(char[] chars) {
            return of(chars, null);
        }

        /**
         * base16
         */
        public static final Table base16Table = Table.of(
                new char[]{
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        'A', 'B', 'C', 'D', 'E', 'F'
                }
        );

        /**
         * base32 (标准填充 '=')
         */
        public static final Table base32Table = Table.of(
                new char[]{
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                        'U', 'V',
                }, '='
        );

        /**
         * base64 (填充 '=')
         */
        public static final Table base64Table = Table.of(
                new char[]{
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                        'U', 'V', 'W', 'X', 'Y', 'Z',
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z',
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        '+', '/'
                }, '='
        );

        /**
         * base64-url (填充 '=')
         */
        public static final Table base64UrlTable = Table.of(
                new char[]{
                        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                        'U', 'V', 'W', 'X', 'Y', 'Z',
                        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                        'u', 'v', 'w', 'x', 'y', 'z',
                        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                        '-', '_'
                }, '='
        );

    }

    /**
     * base16 (hex)
     */
    public static final BNCodec b16 = BNCodec.of(Table.base16Table);
    /**
     * base64 (填充 '=')
     */
    public static final BNCodec b64 = BNCodec.of(Table.base64Table);
    /**
     * base64-url (填充 '=')
     */
    public static final BNCodec b64Url = BNCodec.of(Table.base64UrlTable);


    private final byte[] nTable; // 字母表 （0-255）最多256个
    // 剩余位数不足时填充的字符 ASCII 码 (base64标准需要填充 ‘=’）
    // 为 null 表示不填充
    private final Byte nullablePadding;
    private final int bitsPerChar; // 每编码字符占用位
    private final byte[] decodeLookupTable; //解码查找表
    private final int blockSize; // 每块大小

    /**
     * 构造BaseN编码器
     */
    BNCodec(char[] nTable, Character nullablePadding) {
        this.nTable = buildingSafeBaseNTable(nTable, nullablePadding);
        this.bitsPerChar = initBitsPerChar(nTable.length);
        this.nullablePadding = Optional.ofNullable(nullablePadding)
                .map(c -> (byte) c.charValue())
                .orElse(null);
        // 初始化解码查找表
        this.decodeLookupTable = new byte[256];
        Arrays.fill(decodeLookupTable, (byte) -1);
        for (int i = 0; i < nTable.length; i++) {
            decodeLookupTable[nTable[i] & 0xff] = (byte) i;
        }
        // 块大小
        this.blockSize = lcm(8, bitsPerChar) / bitsPerChar;
    }

    public static BNCodec of(Table table) {
        return new BNCodec(table.nTable, table.nullablePadding);
    }

    public static BNCodec of(char[] nTable) {
        return new BNCodec(nTable, null);
    }

    /**
     * 给定字母表和 填充字符（nullable），校验其，并将字母表转为ASCII byte
     *
     * @param baseNTable      字母表
     * @param nullablePadding 填充字符（nullable）
     * @return ASCII byte
     * @throws IllegalArgumentException 字母表为空、字母表长度小于1
     * @throws IllegalArgumentException 字母表中出现了非ASCII码表示的字符
     * @throws IllegalArgumentException 字母表中包含重复字符
     * @throws IllegalArgumentException 字母表中包含了填充字符（若不为null）
     */
    static byte[] buildingSafeBaseNTable(char[] baseNTable, Character nullablePadding) {
        if (baseNTable == null || baseNTable.length < 2) {
            throw new IllegalArgumentException("字母表长度必须大于1");
        }
        byte[] nTable = new byte[baseNTable.length];
        // 检查字母表中的字符是否唯一
        Set<Character> uniqueChars = new HashSet<>();
        for (int i = 0, baseNTableLength = baseNTable.length; i < baseNTableLength; i++) {
            char b = baseNTable[i];
            if (b > 0xff) {
                throw new IllegalArgumentException(Stf.f("不是ASCII字符: {}", b));
            }
            if (!uniqueChars.add(b)) {
                throw new IllegalArgumentException(Stf.f("字母表包含重复字符: {}", b));
            }
            nTable[i] = (byte) (b & 0xff);
        }
        if (nullablePadding != null && uniqueChars.contains(nullablePadding)) {
            // 不能出现在字母表中
            throw new IllegalArgumentException(Stf.f("填充字符出现在字母表中: {}", nullablePadding));
        }
        return nTable;
    }

    /**
     * 计算每个字符能描述几个 bit<br>
     * 该方法本质是计算字母表中一个字母能最多能描述几个二进制位的数据
     *
     * @param tableLen 字符个数
     * @return 每个字符占用 bit
     */
    private int initBitsPerChar(int tableLen) {
        // fix: 不应当向上取整，Math.cell，比如使用向上取整，会发现62字符表占6位，
        // 该肯定不合法，过多位无法表达（占6位的b62分块无法表达值 0b111111 和 0b111110
        // 所以应当向下取整，即b62应占5位
        // 该方法本质是计算字母表中一个字母能最多能描述几个二进制位的数据而
        int n = tableLen; // 表字符个数，即信号量
        int bits = 0;
        while (n > 1) {
            bits += 1;
            n >>= 1; // 右移
        }
        return bits;
    }

    private boolean hasPadding() {
        return nullablePadding != null;
    }

    /**
     * 计算最小公倍数<br>
     * 用于确定编码块的大小
     */
    private static int lcm(int a, int b) {
        // LCM(a, b) = |a*b| / GCD(a, b)
        return Math.abs(a * b) / gcd(a, b);
    }

    /**
     * 计算最大公约数
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    private static final Charset DEFAULT_CS = StandardCharsets.UTF_8;

    public String encodeToStr(byte[] data, Charset cs) {
        return new String(encode(data), cs);
    }

    public String encodeToStr(byte[] data) {
        return encodeToStr(data, DEFAULT_CS);
    }

    public byte[] decodeFromStr(String encoded, Charset cs) {
        return decode(encoded.getBytes(cs));
    }

    public byte[] decodeFromStr(String encoded) {
        return decodeFromStr(encoded, DEFAULT_CS);
    }

    public String encodeToStr(String data) {
        return encodeToStr(data.getBytes(DEFAULT_CS));
    }

    /**
     * 以 baseN 编码数据
     *
     * @param data 数据
     * @return 编码后数据
     */
    @Override
    public byte[] encode(byte[] data) {
        Objects.requireNonNull(data, "data");
        /*
         * 编码过程：
         * 1. 将输入数据视为一个大的位序列
         * 2. 每次从位序列中取出bitsPerChar位，转换为对应的编码字符
         * 3. 如果最后剩余的位数不足bitsPerChar，则根据配置决定是否需要填充
         */
        if (data.length == 0) {
            return new byte[0];
        }
        try {
            // 计算编码后的长度
            // 公式：⌈(输入字节数 * 8) / bitsPerChar⌉
            int encodedLength = (int) Math.ceil((data.length * 8.0) / bitsPerChar);

            // 如果需要填充，将长度调整为块大小的整数倍
            if (hasPadding()) {
                encodedLength = ((encodedLength + blockSize - 1) / blockSize) * blockSize;
            }

            byte[] encoded = new byte[encodedLength];
            int encodedIndex = 0;

            // 位缓冲区，用于存储待处理的位
            long buffer = 0;
            int bitsInBuffer = 0;

            // 处理每个输入字节
            for (byte b : data) {
                // 将当前字节添加到缓冲区
                // fix：b不与0xff相与时，若b为负数 则在与（buffer << 8)运算时，
                // 会因为隐式的byte转int过程，而将b转为 int的负数，再与buffer做或运算时，
                // 会将 buffer变成负数，娘希匹
                buffer = (buffer << 8) | (b & 0xff);
                bitsInBuffer += 8;

                // 当缓冲区中的位数足够时，提取并编码
                while (bitsInBuffer >= bitsPerChar) {
                    bitsInBuffer -= bitsPerChar;
                    // 从缓冲区提取bitsPerChar位
                    int index = (int) ((buffer >> bitsInBuffer) & ((1L << bitsPerChar) - 1));
                    encoded[encodedIndex++] = nTable[index];
                }
            }

            // 处理剩余的位
            if (bitsInBuffer > 0) {
                // 左移以对齐最后的位
                int index = (int) ((buffer << (bitsPerChar - bitsInBuffer)) & ((1L << bitsPerChar) - 1));
                encoded[encodedIndex++] = nTable[index];
            }

            // 添加填充字符
            if (nullablePadding != null && encodedIndex < encoded.length) {
                Arrays.fill(encoded, encodedIndex, encoded.length, nullablePadding);
            }

            return encoded;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot encode", e);
        }
    }

    /**
     * 以 baseN 解码数据
     *
     * @param encoded 编码的数据
     * @return 数据
     */
    @Override
    public byte[] decode(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        /*
         * 解码byte数组
         * 解码过程：
         * 1. 将每个编码字符转换回其对应的位值
         * 2. 将这些位值重新组合成原始字节
         * 3. 处理可能的填充字符
         */
        if (encoded.length == 0) {
            return new byte[0];
        }

        try {
            // 计算有效的编码字符数（不包括填充字符）
            int effectiveLength = encoded.length;
            if (nullablePadding != null) {
                while (effectiveLength > 0 && encoded[effectiveLength - 1] == nullablePadding) {
                    effectiveLength--;
                }
            }

            // 计算总位数和输出长度
            int totalBits = effectiveLength * bitsPerChar;
            int decodedLength = totalBits / 8; // 整数除法，向下取整
            byte[] decoded = new byte[decodedLength];
            int decodedIndex = 0;

            // 位缓冲区
            long buffer = 0;
            int bitsInBuffer = 0;

            // 处理每个编码字符
            for (int i = 0; i < effectiveLength; i++) {
                byte b = encoded[i];
                // 查找字符对应的值
                int value = decodeLookupTable[b & 0xff];
                if (value == -1) {
                    throw new IllegalArgumentException("非法字符: " + (char) b);
                }

                // 将值添加到缓冲区
                buffer = (buffer << bitsPerChar) | value;
                bitsInBuffer += bitsPerChar;

                // 当缓冲区中有足够的位时，提取一个字节
                while (bitsInBuffer >= 8) {
                    bitsInBuffer -= 8;
                    decoded[decodedIndex++] = (byte) ((buffer >> bitsInBuffer) & 0xff);
                }
            }

            // 验证剩余位
            int expectedRemaining = totalBits % 8;
            if (bitsInBuffer != expectedRemaining) {
                throw new IllegalStateException(
                        Stf.f("剩余位数不一致: 实际={}, 预期={}", bitsInBuffer, expectedRemaining));
            }

            // 剩余位必须全为0，否则数据有误
            if (bitsInBuffer > 0) {
                long mask = (1L << bitsInBuffer) - 1;
                if ((buffer & mask) != 0) {
                    throw new IllegalArgumentException("非法的结束位：存在非零填充位");
                }
            }

            // 检查实际写入字节数
            if (decodedIndex != decodedLength) {
                throw new IllegalStateException(
                        Stf.f("写入字节数不一致: 实际={}, 预期={}", decodedIndex, decodedLength));
            }

            return decoded;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot decode", e);
        }
    }


}