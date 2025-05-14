package io.github.baifangkual.bfk.j.mod.vfs.exception;

/**
 * 虚拟文件系统IO异常类，为方便，该为运行时异常
 *
 * @author baifangkual
 * @since 2024/8/28 v0.0.5
 */
public class VFSIOException extends RuntimeException {

    public VFSIOException() {
    }

    public VFSIOException(String message) {
        super(message);
    }

    public VFSIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public VFSIOException(Throwable cause) {
        super(cause);
    }

    public VFSIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
