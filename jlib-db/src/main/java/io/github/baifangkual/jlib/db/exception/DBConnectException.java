package io.github.baifangkual.jlib.db.exception;


/**
 * 数据源连接失败的异常
 * <p>应表示在连接对象创建失败而导致的异常
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public class DBConnectException extends IllegalStateException {
    public DBConnectException() {
    }

    public DBConnectException(String message) {
        super(message);
    }

    public DBConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBConnectException(Throwable cause) {
        super(cause);
    }


}
