package io.github.baifangkual.jlib.db.exception;

/**
 * @author baifangkual
 * create time 2024/7/11
 * <p>
 * 表示 连接参数异常
 */
public class IllegalConnectionConfigException extends IllegalArgumentException {

    public IllegalConnectionConfigException() {
    }

    public IllegalConnectionConfigException(String message) {
        super(message);
    }

    public IllegalConnectionConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalConnectionConfigException(Throwable cause) {
        super(cause);
    }

}
