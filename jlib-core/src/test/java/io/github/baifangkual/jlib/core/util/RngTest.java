package io.github.baifangkual.jlib.core.util;

import io.github.baifangkual.jlib.core.lang.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author baifangkual
 * @since 2025/5/21
 */
@SuppressWarnings({"CommentedOutCode", "PointlessArithmeticExpression", "RedundantSuppression"})
public class RngTest {

    @Test
    public void test() {
        R<Long> r10 = R.ofFnCallable(() -> Rng.nextFixLenLarge(10)).map(Long::valueOf);
        Assertions.assertDoesNotThrow(() -> r10.unwrap());
        R<Long> r0 = R.ofFnCallable(() -> Long.valueOf(Rng.nextFixLenLarge(0)));
        Assertions.assertThrows(R.UnwrapException.class, r0::unwrap);
        R<Long> r100 = R.ofFnCallable(() -> Rng.nextFixLenLarge(100)).map(Long::valueOf);
        Assertions.assertThrows(R.UnwrapException.class, r100::unwrap);
    }

    @Test
    public void test2() {
        for (int i = 0; i < 100; i++) {
            String num = Rng.nextFixLenLarge(i + 1);
            //System.out.println(num);
            Assertions.assertEquals(num.length(), i + 1);
        }
    }

    @Test
    public void test3() {
        for (int i = 0; i < 1000; i++) {
            int needLen = Rng.nextInt(1, 1000);
            int n2To62 = Rng.nextInt(2, 63);
            String num = Rng.nextFixLenLarge(needLen, n2To62);
            int numLength = num.length();
//            System.out.println(Stf.f("radix: {}, num: {}, nLen: {}, needLen: {}",
//                    n2To62, num, numLength, needLen));
            Assertions.assertEquals(numLength, needLen);
        }
    }

//
//    @Test
//    public void test4() {
//        for (int i = 0; i < 10000; i++) {
//            int needLen = i + 1;
//            int randomRadix = Rng.nextInt(2, 63);
//            int radixNumLen = Radixc.base2len(needLen, randomRadix, 10);
//            String num = Rng.nextFixLenLarge(radixNumLen);
//            int beforeRadixConvertLen = num.length();
//            String afterRadixConvertNum = Radixc.convert(num, 10, randomRadix);
//            int afterRadixConvertLen = afterRadixConvertNum.length();
//            if (afterRadixConvertLen - needLen > 1) {
//                System.out.println(Stf.f("""
//                                需要进制: {}, 需要长度: {}
//                                计算到10进制所需要长度: {}
//                                实际数字（转换前）: {}
//                                实际数字转换前长度: {}
//                                实际数字（转换后）: {}
//                                实际数字转换后长度: {}""",
//                        randomRadix, needLen,
//                        radixNumLen, num,
//                        beforeRadixConvertLen,
//                        afterRadixConvertNum,
//                        afterRadixConvertLen));
//            }
//
//        }
//    }


}
