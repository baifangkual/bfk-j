package io.github.baifangkual.bfk.j.mod.core.panic;

import io.github.baifangkual.bfk.j.mod.core.exception.PanicException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author baifangkual
 * @since 2025/5/3
 */
public class ErrTest {

    @Test
    public void test01() {
        boolean tr = true;
        Assertions.assertThrows(PanicException.class, () -> {
            Err.panicIf(tr, IllegalAccessException::new, "tr is true!");
        });
    }

    @Test
    public void test02() {
        boolean tr = true;
        Assertions.assertThrows(IllegalAccessException.class, () -> {
            Err.realIf(tr, IllegalAccessException::new, "tr is true!");
        });
    }

}
