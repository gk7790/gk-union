package com.gk.psp.callback;

import com.gk.psp.callback.model.PspCallbackResponse;
import org.springframework.http.HttpStatus;

public class PspCallbackException extends RuntimeException {
    private final HttpStatus status;
    private final String responseBody;

    public PspCallbackException(HttpStatus status, String message, String responseBody) {
        super(message);
        this.status = status;
        this.responseBody = responseBody;
    }

    public static PspCallbackException badRequest(String message, String responseBody) {
        return new PspCallbackException(HttpStatus.BAD_REQUEST, message, responseBody);
    }

    public static PspCallbackException unauthorized(String message, String responseBody) {
        return new PspCallbackException(HttpStatus.UNAUTHORIZED, message, responseBody);
    }

    public static PspCallbackException forbidden(String message, String responseBody) {
        return new PspCallbackException(HttpStatus.FORBIDDEN, message, responseBody);
    }

    public static PspCallbackException internalServerError(String message, String responseBody) {
        return new PspCallbackException(HttpStatus.INTERNAL_SERVER_ERROR, message, responseBody);
    }

    public PspCallbackResponse toResponse() {
        return new PspCallbackResponse(status, responseBody);
    }
}
