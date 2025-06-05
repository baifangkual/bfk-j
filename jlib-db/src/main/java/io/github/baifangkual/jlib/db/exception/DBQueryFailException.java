package io.github.baifangkual.jlib.db.exception;


/**
 * @author baifangkual
 * create time 2024/7/15
 * 查询时异常
 */
public class DBQueryFailException extends IllegalStateException {
    public DBQueryFailException() {
    }

    public DBQueryFailException(String message) {
        super(message);
    }

    public DBQueryFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBQueryFailException(Throwable cause) {
        super(cause);
    }

}
