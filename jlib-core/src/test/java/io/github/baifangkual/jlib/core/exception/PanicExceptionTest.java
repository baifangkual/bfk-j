package io.github.baifangkual.jlib.core.exception;

import io.github.baifangkual.jlib.core.panic.PanicException;
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
