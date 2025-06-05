package io.github.baifangkual.jlib.core.codec;

import io.github.baifangkual.jlib.core.util.Rng;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * @author baifangkual
 * @since 2024/6/27
 */
public class BNCodecTest {


    @Test
    public void test() {
        char[] rngChars = new char[256];
        for (int i = 0; i < 200; i++) {
            rngChars[i] = (char) Rng.nextLong();
        }

        int loop = 100;
        BNCodec base16 = BNCodec.of(BNCodec.Table.base16Table);
        BNCodec base32 = BNCodec.of(BNCodec.Table.base32Table);
        BNCodec base64 = BNCodec.of(BNCodec.Table.base64Table);
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();

        while (loop-- > 0) {
            String largeNum = Rng.nextFixLenLarge(30);
            int rngIdx = Rng.nextInt(30);
            char c = largeNum.charAt(rngIdx);
            String rngStr = largeNum.replace(c, rngChars[Rng.nextInt(256)]);
            byte[] rngBytes = rngStr.getBytes(StandardCharsets.UTF_8);

            byte[] javaEncoded = encoder.encode(rngBytes);
            byte[] javaDecoded = decoder.decode(javaEncoded);

            String base16Encoded = base16.encodeToStr(rngBytes);
            String base62Encoded = base32.encodeToStr(rngBytes);
            String base64Encoded = base64.encodeToStr(rngBytes);

            byte[] base64EncodeBytes = base64.encode(rngBytes);

            byte[] base16Decoded = base16.decodeFromStr(base16Encoded);
            byte[] base62Decoded = base32.decodeFromStr(base62Encoded);
            byte[] base64Decoded = base64.decodeFromStr(base64Encoded);

            Assertions.assertArrayEquals(rngBytes, base16Decoded);
            Assertions.assertArrayEquals(rngBytes, base62Decoded);
            Assertions.assertArrayEquals(rngBytes, base64Decoded);
            Assertions.assertArrayEquals(rngBytes, javaDecoded);
            Assertions.assertArrayEquals(base64EncodeBytes, javaEncoded);

//            System.out.println("原始数据 UF8: " + new String(rngBytes, cs));
//            System.out.println("原始数据 Byte: " + Arrays.toString(rngBytes));
//            System.out.println(Stf.f("java b64Encode: {}", Arrays.toString(javaEncoded)));
//            System.out.println(Stf.f("java b64Decode: {}", Arrays.toString(javaDecoded)));
//            System.out.println("Base16 UF8: " + base16Encoded);
//            System.out.println("Base62 UF8: " + base62Encoded);
//            System.out.println("Base64 UF8: " + base64Encoded);
//            System.out.println("Base16 解码 UF8: " + Arrays.toString(base16Decoded));
//            System.out.println("Base62 解码 UF8: " + Arrays.toString(base62Decoded));
//            System.out.println("Base64 解码 UF8: " + Arrays.toString(base64Decoded));
        }
    }

    @Test
    public void test2() {
        BNCodec b8 = BNCodec.of(BNCodec.Table.base32Table);
        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[4];
            for (int j = 0; j < 4; j++) {
                bytes[3 - j] = (byte) ((i >> (j * 8)) & 0xff);
            }
            String s = b8.encodeToStr(bytes);
            byte[] bytes1 = b8.decodeFromStr(s);
            Assertions.assertArrayEquals(bytes, bytes1);
            // System.out.println(Stf.f("rn: {}, b62Encode: {}", i, s));
        }
    }

//    @Test
//    public void test3() {
//        String s = "999";
//        BNCodec b62 = BNCodec.b64Url;
//        String s1 = b62.encodeToStr(s.getBytes(StandardCharsets.UTF_8));
//        byte[] bytes = BNCodec.b64Url.decodeFromStr(s1);
//        Assertions.assertArrayEquals(s.getBytes(StandardCharsets.UTF_8), bytes);
//        System.out.println(s1);
//    }

//    @Test
//    public void test10(){
//        String s = "999";
//        BNCodec b62 = BNCodec.b64;
//        String s1 = b62.encodeToStr(s.getBytes(StandardCharsets.UTF_8));
//        byte[] bytes = BNCodec.b64.decodeFromStr(s1);
//        Assertions.assertArrayEquals(s.getBytes(StandardCharsets.UTF_8), bytes);
//        System.out.println(s1);
//    }
//
//    @Test
//    public void test4() {
//        String s = "999";
//        BNCodec b62 = BNCodec.b16;
//        String s1 = b62.encodeToStr(s.getBytes(StandardCharsets.UTF_8));
//        byte[] bytes = BNCodec.b16.decodeFromStr(s1);
//        Assertions.assertArrayEquals(s.getBytes(StandardCharsets.UTF_8), bytes);
//        System.out.println(s1);
//    }

    private int initBitsPerChar(int tableLen) {
        // 计算每个字符表示的位数
        int n = tableLen; // 表字符个数，即信号量
        int bits = 0;
        while (n > 1) {
            bits += 1;
            n >>= 1; // 右移
        }
        return bits;
    }


    @Test
    public void test6() {
        int alphabetLen = 256;

        for (int i = 2; i <= alphabetLen; i++) {

            double a = Math.log(i) / Math.log(2);
            int bitsPerChar1 = (int) Math.floor(a);
            int bitsPerChar2 = initBitsPerChar(i);
            Assertions.assertEquals(bitsPerChar2, bitsPerChar1);

//            System.out.println(Stf.f("alpLen: {}, bPC1: {}, bPC2: {}, bPC3: {}",
//                    i, bitsPerChar1, bitsPerChar2));

        }


    }
}
