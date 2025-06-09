package io.github.baifangkual.jlib.db.exception;

/**
 * jdbc driver not found
 *
 * @author baifangkual
 * @since 2025/6/9
 */
public class DriverNotFoundException extends IllegalStateException {
    public DriverNotFoundException(String message) {
        super(message);
    }

    public DriverNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
