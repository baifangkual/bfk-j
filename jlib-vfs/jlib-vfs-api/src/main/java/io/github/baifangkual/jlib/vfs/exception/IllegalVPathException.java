package io.github.baifangkual.jlib.vfs.exception;

/**
 * vPath 异常，不应该的状态
 *
 * @author baifangkual
 * @since 2024/8/26 v0.0.5
 */
public class IllegalVPathException extends IllegalStateException {
    public IllegalVPathException() {
    }

    public IllegalVPathException(String s) {
        super(s);
    }

    public IllegalVPathException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalVPathException(Throwable cause) {
        super(cause);
    }
}
