package io.github.baifangkual.bfk.j.mod.core.code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author baifangkual
 * @since 2024/6/24 v0.0.7
 * 工具类，提供 各种 编码 解码 方式,
 * 所有实现，应当有 data == xxxEncode(xxxDecode(data)) 和 data == fromXxxString(toXxxString(data))
 */
public final class Codes {



    /**
     * 使用gzip压缩给定的字节序列
     */
    public static byte[] gzipEncode(byte[] data) {
        preNonNull(data);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            gos.write(data);
            gos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("unable to gzip encode bytes", e);
        }
    }

    /**
     * 使用gzip解码data，data应为已压缩过的数据字节
     */
    public static byte[] gzipDecode(byte[] data) {
        preNonNull(data);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayInputStream bis = new ByteArrayInputStream(data);
             GZIPInputStream gis = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("unable to gzip decode bytes", e);
        }
    }

    /**
     * 以十六进制字面量形式编码给定的字节序列，并以utf8编码构成str
     */
    public static String toHexEncodeString(byte[] data) {
        byte[] hexEncode = hexEncode(data);
        return new String(hexEncode, UTF_8);
    }

    /**
     * 从给定的十六进制字面量值解码得到原始数据字节序列，要求给定的为utf8编码str
     */
    public static byte[] fromHexEncodeString(String hexEncodeString) {
        return hexDecode(hexEncodeString.getBytes(UTF_8));
    }

    /**
     * 以十六进制字面量形式编码给定的字节序列
     */
    public static byte[] hexEncode(byte[] data) {
        preNonNull(data);
        byte[] result = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            byte l = BASE62_CHARS[b >> 4 & 0x0f]; // 高位
            byte r = BASE62_CHARS[b & 0x0f]; // 低位
            result[i * 2] = l;
            result[i * 2 + 1] = r;
        }
        return result;
    }

    /**
     * 从给定的十六进制字面量值解码得到原始数据字节序列
     */
    public static byte[] hexDecode(byte[] data) {
        preNonNull(data);
        byte[] result = new byte[data.length / 2];
        for (int i = 0; i < data.length; i += 2) {
            byte l = data[i];
            byte r = data[i + 1];
            if (l > 'Z') { // 兼容小写字母
                l -= 32;
            }
            if (r > 'Z') { // 兼容小写字母
                r -= 32;
            }
            l = BASE62_LOOKUP[l];
            r = BASE62_LOOKUP[r];
            result[i / 2] = (byte) ((l << 4) | r); // 合并高低位
        }
        return result;
    }

    /**
     * 以base62字面值编码给定的字节序列，返回utf8编码的str
     */
    public static String toBase62EncodeString(byte[] data) {
        byte[] base62Encode = base62Encode(data);
        return new String(base62Encode, UTF_8);
    }

    /**
     * 给定base62字面值str，要求其为utf8编码，返回其表示的实际字节序列值
     */
    public static byte[] fromBase62EncodeString(String base62EncodeString) {
        return base62Decode(base62EncodeString.getBytes(UTF_8));
    }

    /**
     * 以base62字面值编码给定的字节序列
     */
    public static byte[] base62Encode(byte[] data) {
        preNonNull(data);
        byte[] r = convert(data, 256, 62);
        return translate(r, BASE62_CHARS);
    }

    /**
     * 以base62字面值解码字节序列
     */
    public static byte[] base62Decode(byte[] data) {
        preNonNull(data);
        byte[] t = translate(data, BASE62_LOOKUP);
        return convert(t, 62, 256);
    }

    // ============================= PRIVATE ===================================

    private Codes() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private static final byte[] BASE62_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', // pre base16
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
            'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };
    private static final byte[] BASE62_LOOKUP; // 反向查找表

    static {
        //以ASCII字面量字符的二进制值做索引，映射实际的二进制值的值
        BASE62_LOOKUP = new byte['z' + 1];
        for (int i = 0; i < BASE62_CHARS.length; i++) {
            BASE62_LOOKUP[BASE62_CHARS[i]] = (byte) i;
        }
    }

    private static void preNonNull(Object obj) {
        if (obj == null) {
            throw new NullPointerException("data is null");
        }
    }

    /**
     * 估算结果长度, <a href="http://codegolf.stackexchange.com/a/21672">算法来源</a>
     *
     * @param inputLength 输入长度
     * @param sourceBase  源基准长度
     * @param targetBase  目标基准长度
     * @return 估算长度
     */
    private static int estimateOutputLength(int inputLength, int sourceBase, int targetBase) {
        return (int) Math.ceil((Math.log(sourceBase) / Math.log(targetBase)) * inputLength);
    }

    /**
     * 反转数组，会变更原数组<br>
     * 该方法复制自cn.hutool.core.codec.Base62Codec#reverse(byte[], int, int)方法<br>
     *
     * @param array             数组，会变更
     * @param endIndexExclusive 结束位置（不包含）
     * @return 变更后的原数组的引用
     */
    private static byte[] reverse(byte[] array, final int endIndexExclusive) {
        if (array.length == 0) {
            return array;
        }
        int i = 0;
        int j = Math.min(array.length, endIndexExclusive) - 1;
        while (j > i) {
            // swap
            byte tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
            j--;
            i++;
        }
        return array;
    }

    /**
     * 使用定义的字母表从源基准到目标基准<br>
     * 该方法复制自cn.hutool.core.codec.Base62Codec#convert(byte[], int, int)方法<br>
     * 该方法本质是单位信号量转换方法
     *
     * @param message    消息bytes
     * @param sourceBase 源基准长度
     * @param targetBase 目标基准长度
     * @return 计算结果
     */
    private static byte[] convert(byte[] message, int sourceBase, int targetBase) {
        // 计算结果长度，算法来自：http://codegolf.stackexchange.com/a/21672
        final int estimatedLength = estimateOutputLength(message.length, sourceBase, targetBase);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(estimatedLength);
        byte[] source = message;
        while (source.length > 0) {
            final ByteArrayOutputStream quotient = new ByteArrayOutputStream(source.length);
            int remainder = 0;
            for (byte b : source) {
                final int accumulator = (b & 0xFF) + remainder * sourceBase;
                final int digit = (accumulator - (accumulator % targetBase)) / targetBase;
                remainder = accumulator % targetBase;
                if (quotient.size() > 0 || digit > 0) {
                    quotient.write(digit);
                }
            }
            out.write(remainder);
            source = quotient.toByteArray();
        }
        // pad output with zeroes corresponding to the number of leading zeroes in the message
        for (int i = 0; i < message.length - 1 && message[i] == 0; i++) {
            out.write(0);
        }
        byte[] byteArray = out.toByteArray();
        return reverse(byteArray, byteArray.length);
    }

    /**
     * 按照字典转换bytes
     *
     * @param indices    内容
     * @param dictionary 字典
     * @return 转换值
     */
    private static byte[] translate(byte[] indices, byte[] dictionary) {
        final byte[] translation = new byte[indices.length];
        for (int i = 0; i < indices.length; i++) {
            translation[i] = dictionary[indices[i]];
        }
        return translation;
    }


}
