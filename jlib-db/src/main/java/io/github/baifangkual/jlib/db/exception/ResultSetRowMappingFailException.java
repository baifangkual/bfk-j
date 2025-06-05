package io.github.baifangkual.jlib.db.exception;


/**
 * @author baifangkual
 * create time 2024/7/15
 * 行转换异常
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
