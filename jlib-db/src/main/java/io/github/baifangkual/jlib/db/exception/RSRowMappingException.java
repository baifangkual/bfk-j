package io.github.baifangkual.jlib.db.exception;


/**
 * jdbc {@link java.sql.ResultSet} 读取值过程错误导致的异常-行转换异常
 *
 * @author baifangkual
 * @since 2024/7/15
 */
public class RSRowMappingException extends IllegalStateException {
    public RSRowMappingException() {
    }

    public RSRowMappingException(String message) {
        super(message);
    }

    public RSRowMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RSRowMappingException(Throwable cause) {
        super(cause);
    }

}
