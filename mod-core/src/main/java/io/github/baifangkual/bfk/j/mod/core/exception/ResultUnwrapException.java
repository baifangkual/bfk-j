package io.github.baifangkual.bfk.j.mod.core.exception;

import io.github.baifangkual.bfk.j.mod.core.model.R;

/**
 * 表达{@link R}类型解包时的异常
 */
public class ResultUnwrapException extends RuntimeException {
    public ResultUnwrapException(String message) {
        super(message);
    }

    public ResultUnwrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultUnwrapException(Throwable cause) {
        super(cause);
    }
}
