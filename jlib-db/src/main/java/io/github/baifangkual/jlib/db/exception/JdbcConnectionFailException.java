package io.github.baifangkual.jlib.db.exception;


/**
 * 数据源连接失败的异常
 *
 * @author baifangkual
 * @since 2024/7/11
 */
public class JdbcConnectionFailException extends IllegalStateException {
    public JdbcConnectionFailException() {
    }

    public JdbcConnectionFailException(String message) {
        super(message);
    }

    public JdbcConnectionFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public JdbcConnectionFailException(Throwable cause) {
        super(cause);
    }


}
