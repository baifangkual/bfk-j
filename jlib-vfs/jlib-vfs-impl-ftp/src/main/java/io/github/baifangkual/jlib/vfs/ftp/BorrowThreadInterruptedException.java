package io.github.baifangkual.jlib.vfs.ftp;

/**
 * 借用线程中断异常，专门形容外界使线程中断而导致的异常，方便专有捕获处理
 *
 * @author baifangkual
 * @since 2024/9/18 v0.0.5
 */
public class BorrowThreadInterruptedException extends RuntimeException {
    public BorrowThreadInterruptedException(String message) {
        super(message);
    }

    public BorrowThreadInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BorrowThreadInterruptedException(Throwable cause) {
        super(cause);
    }

    public BorrowThreadInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BorrowThreadInterruptedException() {
    }
}
