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
        R<Long, RuntimeException> r10 = R.ofSupplier(() -> Long.valueOf(Rng.rollFixLenLarge(10)));
        Assertions.assertDoesNotThrow(() -> r10.unwrap());
        R<Long, RuntimeException> r0 = R.ofSupplier(() -> Long.valueOf(Rng.rollFixLenLarge(0)));
        Assertions.assertThrows(R.UnwrapException.class, r0::unwrap);
        R<Long, RuntimeException> r100 = R.ofSupplier(() -> Long.valueOf(Rng.rollFixLenLarge(100)));
        Assertions.assertThrows(R.UnwrapException.class, r100::unwrap);
    }

    @Test
    public void test2() {
        for (int i = 0; i < 100; i++) {
            String num = Rng.rollFixLenLarge(i + 1);
            //System.out.println(num);
            Assertions.assertEquals(num.length(), i + 1);
        }
    }


}
