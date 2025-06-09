package io.github.baifangkual.jlib.db.exception;


/**
 * 查询时异常包装，语法错误致使查询失败等
 *
 * @author baifangkual
 * @since 2024/7/15
 */
public class DBQueryException extends IllegalStateException {
    public DBQueryException() {
    }

    public DBQueryException(String message) {
        super(message);
    }

    public DBQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBQueryException(Throwable cause) {
        super(cause);
    }

}
