package io.github.baifangkual.jlib.db.exception;

/**
 * @author baifangkual
 * create time 2024/7/29
 */
public class ConnectionCloseFailException extends IllegalStateException {

    public ConnectionCloseFailException() {
    }

    public ConnectionCloseFailException(String s) {
        super(s);
    }

    public ConnectionCloseFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionCloseFailException(Throwable cause) {
        super(cause);
    }
}
