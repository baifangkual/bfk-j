package io.github.baifangkual.jlib.db.exception;


/**
 * @author baifangkual
 * create time 2024/7/11
 * 数据源连接失败的异常
 */
public class DataSourceConnectionFailException extends IllegalStateException {
    public DataSourceConnectionFailException() {
    }

    public DataSourceConnectionFailException(String message) {
        super(message);
    }

    public DataSourceConnectionFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSourceConnectionFailException(Throwable cause) {
        super(cause);
    }


}
