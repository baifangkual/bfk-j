package io.github.baifangkual.jlib.vfs.minio;

import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import lombok.Getter;

/**
 * 错误响应异常，运行时异常，包装了 {@link ErrorResponseException}
 *
 * @author baifangkual
 * @since 2024/9/5
 */
@Getter
public class ErrorResponseRuntimeException extends IllegalStateException {

    private final ErrorResponse errorResponse;


    public ErrorResponseRuntimeException(ErrorResponseException exception) {
        super(exception);
        errorResponse = exception.errorResponse();
    }

}
