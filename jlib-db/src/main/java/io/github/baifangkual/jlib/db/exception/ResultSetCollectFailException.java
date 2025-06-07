package io.github.baifangkual.jlib.db.exception;

/**
 * jdbc {@link java.sql.ResultSet} 读取值过程错误导致的异常
 *
 * @author baifangkual
 * @since 2024/7/25
 */
public class ResultSetCollectFailException extends ResultSetRowMappingFailException {
    public ResultSetCollectFailException() {
    }

    public ResultSetCollectFailException(String message) {
        super(message);
    }

    public ResultSetCollectFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultSetCollectFailException(Throwable cause) {
        super(cause);
    }

}
