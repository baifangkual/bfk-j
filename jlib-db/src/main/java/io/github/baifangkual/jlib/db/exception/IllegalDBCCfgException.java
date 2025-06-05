package io.github.baifangkual.jlib.db.exception;

/**
 * @author baifangkual
 * create time 2024/7/11
 * <p>
 * 表示 连接参数异常
 */
public class IllegalDBCCfgException extends IllegalArgumentException {

    public IllegalDBCCfgException() {
    }

    public IllegalDBCCfgException(String message) {
        super(message);
    }

    public IllegalDBCCfgException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDBCCfgException(Throwable cause) {
        super(cause);
    }

}
