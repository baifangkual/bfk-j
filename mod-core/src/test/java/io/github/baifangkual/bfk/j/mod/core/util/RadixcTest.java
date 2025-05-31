package io.github.baifangkual.bfk.j.mod.core.util;

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
}
