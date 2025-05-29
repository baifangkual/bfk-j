package io.github.baifangkual.bfk.j.mod.core.codec;

import io.github.baifangkual.bfk.j.mod.core.util.Stf;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * BaseN编解码器。
 * 支持任意进制（N）的编解码，其中N由字母表大小决定。
 * 常见的如Base16(hex)、Base32、Base58、Base62、Base64等都可以用这个编码器实现。
 *
 * @author baifangkual
 * @since 2025/5/30
 */
public final class BaseN {

    /**
     * 字母表配置
     *
     * @param alphabet      字母表字符集
     * @param padChar       填充字符(可选)
     * @param strictPadding 是否强制填充(可选)
     */
    public record AlphabetConfig(byte[] alphabet, Byte padChar, boolean strictPadding) {
        public AlphabetConfig {
            // 验证字母表有效性
            validateAlphabet(alphabet);
        }

        private static void validateAlphabet(byte[] alphabet) {
            if (alphabet == null || alphabet.length < 2) {
                throw new IllegalArgumentException("字母表长度必须大于1");
            }

            // 检查字母表中的字符是否唯一
            Set<Byte> uniqueChars = new HashSet<>();
            for (byte b : alphabet) {
                if (!uniqueChars.add(b)) {
                    throw new IllegalArgumentException("字母表包含重复字符");
                }
            }
        }
    }

    private final AlphabetConfig config;
    private final int bitsPerChar; // 每个编码字符表示的位数
    private final byte[] decodeTable; // 解码查找表

    /**
     * 构造BaseN编码器
     *
     * @param config 字母表配置
     */
    public BaseN(AlphabetConfig config) {
        this.config = config;
        // 计算每个编码字符能表示的位数
        // 例如：对于Base64(alphabet.length=64)，log2(64)=6，表示每个编码字符使用6位
        this.bitsPerChar = (int) Math.floor(Math.log(config.alphabet.length) / Math.log(2));

        // 初始化解码查找表
        this.decodeTable = new byte[256]; // 支持所有ASCII字符
        Arrays.fill(decodeTable, (byte) -1); // 默认填充-1表示非法字符
        for (int i = 0; i < config.alphabet.length; i++) {
            decodeTable[config.alphabet[i] & 0xFF] = (byte) i;
        }
    }

    /**
     * 编码byte数组
     * 编码过程：
     * 1. 将输入数据视为一个大的位序列
     * 2. 每次从位序列中取出bitsPerChar位，转换为对应的编码字符
     * 3. 如果最后剩余的位数不足bitsPerChar，则根据配置决定是否需要填充
     */
    public byte[] encode(byte[] data) {
        Objects.requireNonNull(data, "data");
        if (data.length == 0) {
            return new byte[0];
        }
        try {
            // 计算编码后的长度
            // 公式：⌈(输入字节数 * 8) / bitsPerChar⌉
            int encodedLength = (int) Math.ceil((data.length * 8.0) / bitsPerChar);

            // 如果需要填充，将长度调整为块大小的整数倍
            if (config.strictPadding && config.padChar != null) {
                int blockSize = lcm(8, bitsPerChar) / bitsPerChar; // 计算块大小
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
                buffer = (buffer << 8) | b;
                bitsInBuffer += 8;

                // 当缓冲区中的位数足够时，提取并编码
                while (bitsInBuffer >= bitsPerChar) {
                    bitsInBuffer -= bitsPerChar;
                    // 从缓冲区提取bitsPerChar位
                    int index = (int) ((buffer >> bitsInBuffer) & ((1L << bitsPerChar) - 1));
                    encoded[encodedIndex++] = config.alphabet[index];
                }
            }

            // 处理剩余的位
            if (bitsInBuffer > 0) {
                // 左移以对齐最后的位
                int index = (int) ((buffer << (bitsPerChar - bitsInBuffer)) & ((1L << bitsPerChar) - 1));
                encoded[encodedIndex++] = config.alphabet[index];
            }

            // 添加填充字符
            if (config.padChar != null && encodedIndex < encoded.length) {
                Arrays.fill(encoded, encodedIndex, encoded.length, config.padChar);
            }

            return encoded;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot encode data", e);
        }
    }

    /**
     * 解码byte数组
     * 解码过程：
     * 1. 将每个编码字符转换回其对应的位值
     * 2. 将这些位值重新组合成原始字节
     * 3. 处理可能的填充字符
     */
    public byte[] decode(byte[] encoded) {
        Objects.requireNonNull(encoded, "encoded");
        if (encoded.length == 0) {
            return new byte[0];
        }

        try {
            // 计算有效的编码字符数（不包括填充字符）
            int effectiveLength = encoded.length;
            if (config.padChar != null) {
                while (effectiveLength > 0 && encoded[effectiveLength - 1] == config.padChar) {
                    effectiveLength--;
                }
            }

            // 计算解码后的长度
            // 公式：向上取整，确保有足够空间存储不完整的最后一个字节
            int decodedLength = (int) Math.ceil((effectiveLength * bitsPerChar) / 8.0);
            byte[] decoded = new byte[decodedLength];
            int decodedIndex = 0;

            // 位缓冲区
            long buffer = 0;
            int bitsInBuffer = 0;

            // 处理每个编码字符
            for (int i = 0; i < effectiveLength; i++) {
                byte b = encoded[i];
                // 查找字符对应的值
                int value = decodeTable[b & 0xFF];
                if (value == -1) {
                    throw new IllegalArgumentException("发现非法字符: " + (char) b);
                }

                // 将值添加到缓冲区
                buffer = (buffer << bitsPerChar) | value;
                bitsInBuffer += bitsPerChar;

                // 当缓冲区中有足够的位时，提取一个字节
                while (bitsInBuffer >= 8) {
                    bitsInBuffer -= 8;
                    decoded[decodedIndex++] = (byte) ((buffer >> bitsInBuffer) & 0xFF);
                }
            }

            // 验证是否有未处理完的位
            if (bitsInBuffer > 0) {
                if (config.strictPadding && config.padChar == null) {
                    throw new IllegalArgumentException("数据长度不正确");
                }
                // 只有当剩余位数足够形成有效数据时才处理最后的不完整字节
                // 例如：对于6位的编码（如Base64），如果剩余4位，这4位是有效的数据位
                // 而不是仅仅因为编码字符的位数不能被8整除而产生的填充位
                if (bitsInBuffer >= Math.ceil(bitsPerChar / 2.0)) {
                    decoded[decodedIndex++] = (byte) ((buffer << (8 - bitsInBuffer)) & 0xFF);
                }
            }

            // 如果实际使用的长度小于分配的长度，返回一个裁剪后的数组
            return decodedIndex < decodedLength ? Arrays.copyOf(decoded, decodedIndex) : decoded;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    Stf.f("cannot decode data"), e);
        }
    }

    public String encodeString(byte[] data) {
        return new String(encode(data), StandardCharsets.UTF_8);
    }

    public String decodeString(byte[] data) {
        return new String(decode(data), StandardCharsets.UTF_8);
    }

    public byte[] stringDecode(String data) {
        return decode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算最小公倍数
     * 用于确定编码块的大小
     */
    private static int lcm(int a, int b) {
        return (a * b) / gcd(a, b);
    }

    /**
     * 计算最大公约数
     * 使用欧几里得算法
     */
    private static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // 示例用法
    public static void main(String[] args) {
        // 创建不同进制的编解码器
        BaseN base16 = new BaseN(Alphabets.BASE16);
        BaseN base62 = new BaseN(Alphabets.BASE62);
        BaseN base64 = new BaseN(Alphabets.BASE64);

        // 测试数据
        //String testString = "12";
        byte[] testData = new byte[]{15};

        Base64.Encoder encoder = Base64.getEncoder();
        byte[] enc = encoder.encode(testData);
        System.out.println(Stf.f("java b64Encode: {}", new String(enc)));

        // 编码
        String base16Encoded = base16.encodeString(testData);
        String base62Encoded = base62.encodeString(testData);
        String base64Encoded = base64.encodeString(testData);

        // 解码
        byte[] base16Decoded = base16.stringDecode(base16Encoded);
        byte[] base62Decoded = base62.stringDecode(base62Encoded);
        byte[] base64Decoded = base64.stringDecode(base64Encoded);

        // 输出结果
        // System.out.println("原始数据: " + testString);
        System.out.println("原始数据Byte: " + Arrays.toString(testData));
        System.out.println("Base16: " + base16Encoded);
        System.out.println("Base62: " + base62Encoded);
        System.out.println("Base64: " + base64Encoded);

        System.out.println("\nBase16 解码: " + Arrays.toString(base16Decoded));
        System.out.println("Base62 解码: " + Arrays.toString(base62Decoded));
        System.out.println("Base64 解码: " + Arrays.toString(base64Decoded));

    }

    /**
     * 预定义的字母表配置
     */
    public static class Alphabets {
        // Base16 (Hex)
        public static final AlphabetConfig BASE16 = new AlphabetConfig(
                "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII),
                null,
                false
        );

        // Base32
        public static final AlphabetConfig BASE32 = new AlphabetConfig(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".getBytes(StandardCharsets.US_ASCII),
                (byte) '=',
                true
        );

        // Base64
        public static final AlphabetConfig BASE64 = new AlphabetConfig(
                ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                 "abcdefghijklmnopqrstuvwxyz" +
                 "0123456789+/").getBytes(StandardCharsets.US_ASCII),
                (byte) '=',
                true
        );

        // Base62
        public static final AlphabetConfig BASE62 = new AlphabetConfig(
                ("0123456789" +
                 "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                 "abcdefghijklmnopqrstuvwxyz").getBytes(StandardCharsets.US_ASCII),
                null,
                false
        );
    }
}