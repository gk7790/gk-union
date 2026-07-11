package com.gk.openapi.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final String detailCode;

    public ApiException(ApiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailCode = null;
    }

    public ApiException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detailCode = null;
    }

    public ApiException(ApiErrorCode errorCode, String detailCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detailCode = detailCode;
    }

    public ApiException(ApiErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detailCode = null;
    }

    public ApiException(ApiErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detailCode = null;
    }
}
