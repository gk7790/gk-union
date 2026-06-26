package com.gk.psp.callback.model;

import org.springframework.http.HttpStatus;

public record PspCallbackResponse(HttpStatus status, String body) {
    public static final String DEFAULT_SUCCESS_BODY = "success";
    public static final String DEFAULT_FAIL_BODY = "fail";

    public PspCallbackResponse {
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        body = defaultBody(body, DEFAULT_FAIL_BODY);
    }

    public static PspCallbackResponse ok(String body) {
        return new PspCallbackResponse(HttpStatus.OK, defaultBody(body, DEFAULT_SUCCESS_BODY));
    }

    public static PspCallbackResponse badRequest(String body) {
        return new PspCallbackResponse(HttpStatus.BAD_REQUEST, body);
    }

    public static PspCallbackResponse unauthorized(String body) {
        return new PspCallbackResponse(HttpStatus.UNAUTHORIZED, body);
    }

    public static PspCallbackResponse forbidden(String body) {
        return new PspCallbackResponse(HttpStatus.FORBIDDEN, body);
    }

    public static PspCallbackResponse internalServerError(String body) {
        return new PspCallbackResponse(HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    private static String defaultBody(String body, String fallback) {
        return body == null || body.isBlank() ? fallback : body;
    }
}
