package io.github.baifangkual.jlib.db.exception;


/**
 * jdbc {@link java.sql.ResultSet} 读取值过程错误导致的异常-行转换异常
 *
 * @author baifangkual
 * @since 2024/7/15
 */
public class ResultSetRowMappingFailException extends IllegalStateException {
    public ResultSetRowMappingFailException() {
    }

    public ResultSetRowMappingFailException(String message) {
        super(message);
    }

    public ResultSetRowMappingFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultSetRowMappingFailException(Throwable cause) {
        super(cause);
    }

}
