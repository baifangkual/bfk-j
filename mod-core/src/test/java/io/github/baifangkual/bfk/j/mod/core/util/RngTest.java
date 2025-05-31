package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.lang.R;
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
        for (int i = 0; i < 100; i++) {
            int n2To62 = Rng.nextInt(2, 63);
            String num = Rng.nextFixLenLarge(i + 1, n2To62);
            int numLength = num.length();
            System.out.println(Stf.f("radix: {}, num: {}, numLen: {}, needLen: {}", n2To62, num, numLength, i + 1));
        }
    }


}
