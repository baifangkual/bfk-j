package io.github.baifangkual.bfk.j.mod.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author baifangkual
 * @since 2024/11/15
 */
@Slf4j
public class PanicExceptionTest {

    @Test
    public void testPanicExceptionWrapMessage() {
        Throwable cause = new RuntimeException("message");
        PanicException wrap = PanicException.wrap(cause);
        Assertions.assertThrows(PanicException.class, () -> {
            throw wrap;
        });
    }

}
