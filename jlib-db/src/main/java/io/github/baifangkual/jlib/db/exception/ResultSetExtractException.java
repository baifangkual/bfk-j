package io.github.baifangkual.jlib.db.exception;

/**
 * jdbc {@link java.sql.ResultSet} 读取值过程错误导致的异常
 *
 * @author baifangkual
 * @since 2024/7/25
 */
public class ResultSetExtractException extends RSRowMappingException {
    public ResultSetExtractException() {
    }

    public ResultSetExtractException(String message) {
        super(message);
    }

    public ResultSetExtractException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultSetExtractException(Throwable cause) {
        super(cause);
    }

}
