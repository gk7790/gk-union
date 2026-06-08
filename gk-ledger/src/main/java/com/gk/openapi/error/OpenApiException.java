package com.gk.openapi.error;

import lombok.Getter;

@Getter
public class OpenApiException extends RuntimeException {
    private final ApiErrorCode errorCode;

    public OpenApiException(ApiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public OpenApiException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
