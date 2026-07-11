package com.gk.payment.domain.error;

import lombok.Getter;

@Getter
public enum PaymentErrorCode {
    INVALID_REQUEST("Invalid request"), INVALID_AMOUNT("Invalid amount"),
    UNSUPPORTED_METHOD("Unsupported method"), INSUFFICIENT_BALANCE("Insufficient balance"),
    ORDER_NOT_FOUND("Order not found"), ORDER_STATUS_INVALID("Order status invalid"),
    SERVICE_NOT_READY("Service is not ready"), SYSTEM_ERROR("System error");

    private final String message;
    PaymentErrorCode(String message) { this.message = message; }
}
