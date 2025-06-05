package io.github.baifangkual.jlib.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author baifangkual
 * @since 2025/5/31
 */
@SuppressWarnings({"SpellCheckingInspection", "CodeBlock2Expr"})
public class RadixcTest {


    @SuppressWarnings("DataFlowIssue")
    @Test
    public void testRadix() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Radixc.convert(null, 1, 2);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Radixc.convert("     ", 1, 2);
        });
        Assertions.assertEquals("-10", Radixc.convert("-10", 10, 10));
        Assertions.assertEquals(Radixc.convert(0, 10), Radixc.convert(0, 10));
        Assertions.assertEquals("1", Radixc.convert(1, 10));
        Assertions.assertEquals("FF", Radixc.convert(255, 16));
        Assertions.assertEquals("FFFF", Radixc.convert(0xffff, 16));
        Assertions.assertEquals("10", Radixc.convert(8, 8));
        Assertions.assertEquals("10", Radixc.convert(8 + 8, 16));
        Assertions.assertEquals("10", Radixc.convert(62, 62));
        Assertions.assertEquals("z", Radixc.convert(61, 62));
    }

    @Test
    public void testRadix2() {
        int loop = 10000;

        for (int i = 0; i < loop; i++) {

            int rRadix = Rng.nextInt(2, 63);
            int r0Or1Or2 = Rng.nextInt(3);
            if (r0Or1Or2 == 0) {
                long l = Rng.nextLong();
                String v = Radixc.convert(l, rRadix);
                String v2 = Radixc.convert(v, rRadix, 10);
                Assertions.assertEquals(v2, String.valueOf(l));
            }
            if (r0Or1Or2 == 1) {
                int rInt = Rng.nextInt();
                String v = Radixc.convert(rInt, rRadix);
                String v2 = Radixc.convert(v, rRadix, 10);
                Assertions.assertEquals(v2, String.valueOf(rInt));
            }
            if (r0Or1Or2 == 2) {
                int i1 = Rng.nextInt(1, 1000);
                String s = Rng.nextFixLenLarge(i1, rRadix);
                int nRRadix = Rng.nextInt(2, 63);
                String v2 = Radixc.convert(s, rRadix, nRRadix);
                String v3 = Radixc.convert(v2, nRRadix, rRadix);
                Assertions.assertEquals(v3, s);
            }
        }
    }

//    @Test
//    public void testRadix3() {
//        String s = "-1w0767";
//        String convert = Radixc.convert(s, 60, 10);
//        System.out.println(convert);
//    }
}
