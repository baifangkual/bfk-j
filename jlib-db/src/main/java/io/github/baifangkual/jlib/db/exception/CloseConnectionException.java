package io.github.baifangkual.jlib.db.exception;

/**
 * 数据库连接关闭过程的异常包装
 *
 * @author baifangkual
 * @since 2024/7/29
 */
public class CloseConnectionException extends IllegalStateException {

    public CloseConnectionException() {
    }

    public CloseConnectionException(String s) {
        super(s);
    }

    public CloseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloseConnectionException(Throwable cause) {
        super(cause);
    }
}
