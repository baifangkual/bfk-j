package io.github.baifangkual.jlib.db.exception;


/**
 * @author baifangkual
 * create time 2024/10/25
 * <p>
 * 删除表发生异常的表达
 */
public class DropTableFailException extends IllegalStateException {
    public DropTableFailException(String message) {
        super(message);
    }

    public DropTableFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public DropTableFailException(Throwable cause) {
        super(cause);
    }


    public DropTableFailException() {
    }
}
