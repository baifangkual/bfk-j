package io.github.baifangkual.jlib.vfs.minio;

/**
 * 表示minio的异常的运行时包装
 *
 * @author baifangkual
 * @since 2024/9/2
 */
public class MinioExceptionRuntimeWrap extends RuntimeException {

    public MinioExceptionRuntimeWrap() {
    }

    public MinioExceptionRuntimeWrap(String message) {
        super(message);
    }

    public MinioExceptionRuntimeWrap(String message, Throwable cause) {
        super(message, cause);
    }

    public MinioExceptionRuntimeWrap(Throwable cause) {
        super(cause);
    }

    public MinioExceptionRuntimeWrap(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
