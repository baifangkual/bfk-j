package io.github.baifangkual.bfk.j.mod.core.exception;

import io.github.baifangkual.bfk.j.mod.core.model.R;

/**
 * 表达{@link R}类型的构造过程的状态异常，该异常发生在{@link R}的构造过程中
 */
public class ResultWrapException extends RuntimeException {
    public ResultWrapException(String message) {
        super(message);
    }
}
