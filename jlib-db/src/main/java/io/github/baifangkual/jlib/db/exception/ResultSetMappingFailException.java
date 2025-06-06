package io.github.baifangkual.jlib.db.exception;

/**
 * jdbc {@link java.sql.ResultSet} 读取值过程错误导致的异常
 *
 * @author baifangkual
 * @since 2024/7/25
 */
public class ResultSetMappingFailException extends ResultSetRowMappingFailException {
    public ResultSetMappingFailException() {
    }

    public ResultSetMappingFailException(String message) {
        super(message);
    }

    public ResultSetMappingFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultSetMappingFailException(Throwable cause) {
        super(cause);
    }

}
