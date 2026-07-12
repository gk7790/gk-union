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

    public PspCallbackResponse toResponse() {
        return new PspCallbackResponse(status, responseBody);
    }
}
