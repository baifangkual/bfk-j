package io.github.baifangkual.jlib.db.exception;

/**
 * @author baifangkual
 * create time 2024/7/25
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
