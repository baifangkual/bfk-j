package io.github.baifangkual.bfk.j.mod.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author baifangkual
 * create time 2024/11/15
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

    @Test
    public void testPanicExceptionLogMessage() {
        Throwable cause = new RuntimeException("message");
        PanicException wrap = PanicException.wrap(cause);
        log.error(wrap.getMessage(), wrap);
    }

    @Test
    public void testPanicExceptionLogMessage02() {
        Throwable cause = new IOException("ioErr");
        PanicException wrap = PanicException.wrap(cause);
        log.error(wrap.getMessage(), wrap);
    }

}
