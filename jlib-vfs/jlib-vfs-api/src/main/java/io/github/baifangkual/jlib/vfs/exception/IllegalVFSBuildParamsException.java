package io.github.baifangkual.jlib.vfs.exception;

/**
 * vfs构造异常，由外界非法/错误参数导致的构造失败，均属于此，仅覆盖预定义的各种非法参数
 *
 * @author baifangkual
 * @since 2024/8/26 v0.0.5
 */
public class IllegalVFSBuildParamsException extends VFSBuildingFailException {
    public IllegalVFSBuildParamsException(String message) {
        super(message);
    }

    public IllegalVFSBuildParamsException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalVFSBuildParamsException(Throwable cause) {
        super(cause);
    }

    public IllegalVFSBuildParamsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public IllegalVFSBuildParamsException() {
    }
}
