package io.github.baifangkual.jlib.db.exception;

/**
 * 错误的连接参数导致的-连接参数异常(外界提供的连接参数格式类型等不对时）
 *
 * @author baifangkual
 * @since 2024/7/11
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
