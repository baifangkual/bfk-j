package io.github.baifangkual.bfk.j.mod.core.exception;

import io.github.baifangkual.bfk.j.mod.core.model.R;

/**
 * 表达{@link R}类型解包时的异常，该异常发生在{@link R}构造后的运行时
 */
public class ResultUnwrapException extends RuntimeException {
    public ResultUnwrapException(String message) {
        super(message);
    }
}
