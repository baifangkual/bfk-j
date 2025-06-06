package io.github.baifangkual.jlib.db.exception;


/**
 * 查询时异常包装，语法错误致使查询失败等
 *
 * @author baifangkual
 * @since 2024/7/15
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
