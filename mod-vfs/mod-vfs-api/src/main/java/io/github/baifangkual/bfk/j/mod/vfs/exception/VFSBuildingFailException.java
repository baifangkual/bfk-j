package io.github.baifangkual.bfk.j.mod.vfs.exception;

/**
 * vfs实例构造异常，大的形容范围，凡在构造阶段出现异常，均应声明或包装至该
 *
 * @author baifangkual
 * @since 2024/8/26 v0.0.5
 */
public class VFSBuildingFailException extends RuntimeException {
    public VFSBuildingFailException(String message) {
        super(message);
    }

    public VFSBuildingFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public VFSBuildingFailException(Throwable cause) {
        super(cause);
    }

    public VFSBuildingFailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public VFSBuildingFailException() {
    }
}
