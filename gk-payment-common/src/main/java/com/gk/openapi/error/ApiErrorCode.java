package com.gk.openapi.error;

import lombok.Getter;

@Getter
public enum ApiErrorCode {
    SUCCESS("success"),
    INVALID_REQUEST("Invalid request"),
    INVALID_APP("Invalid app"),
    APP_DISABLED("App is disabled"),
    MERCHANT_DISABLED("Merchant is disabled"),
    INVALID_IP("Invalid client IP"),
    INVALID_TIMESTAMP("Invalid timestamp"),
    REPLAY_REQUEST("Replay request"),
    INVALID_SIGNATURE("Invalid signature"),
    UNSUPPORTED_SIGN_TYPE("Unsupported sign type"),
    DUPLICATE_REQUEST("Duplicate request"),
    INVALID_AMOUNT("Invalid amount"),
    UNSUPPORTED_METHOD("Unsupported method"),
    INSUFFICIENT_BALANCE("Insufficient balance"),
    ORDER_NOT_FOUND("Order not found"),
    ORDER_STATUS_INVALID("Order status invalid"),
    SERVICE_NOT_READY("Service is not ready"),
    SYSTEM_ERROR("System error");

    private final String message;

    ApiErrorCode(String message) {
        this.message = message;
    }
}
