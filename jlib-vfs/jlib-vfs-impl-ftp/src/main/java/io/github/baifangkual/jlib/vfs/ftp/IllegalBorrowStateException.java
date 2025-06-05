package io.github.baifangkual.jlib.vfs.ftp;

/**
 * 非法借用状态异常，专门表示可被借用的FTPCli状态异常
 *
 * @author baifangkual
 * @since 2024/9/18 v0.0.5
 */
public class IllegalBorrowStateException extends IllegalStateException {
    public IllegalBorrowStateException() {
    }

    public IllegalBorrowStateException(String s) {
        super(s);
    }

    public IllegalBorrowStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalBorrowStateException(Throwable cause) {
        super(cause);
    }
}
