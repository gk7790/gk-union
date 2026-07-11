package com.gk.openapi.error;

public final class ApiErrorMessage {
    public static final String AMOUNT_NOT_SUPPORTED = "Amount is not supported";
    public static final String METHOD_NOT_SUPPORTED = ApiErrorCode.UNSUPPORTED_METHOD.getMessage();
    public static final String SERVICE_NOT_READY = ApiErrorCode.SERVICE_NOT_READY.getMessage();
    public static final String INVALID_AMOUNT = ApiErrorCode.INVALID_AMOUNT.getMessage();
    public static final String INVALID_REQUEST = ApiErrorCode.INVALID_REQUEST.getMessage();
    public static final String DUPLICATE_REQUEST = ApiErrorCode.DUPLICATE_REQUEST.getMessage();
    public static final String INSUFFICIENT_BALANCE = ApiErrorCode.INSUFFICIENT_BALANCE.getMessage();
    public static final String SYSTEM_ERROR = ApiErrorCode.SYSTEM_ERROR.getMessage();

    private ApiErrorMessage() {
    }
}
