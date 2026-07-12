package com.gk.payment.domain.error;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final PaymentErrorCode errorCode;
    private final String detailCode;

    public PaymentException(PaymentErrorCode code, String message) { this(code, null, message, null); }
    public PaymentException(PaymentErrorCode code, Throwable cause) { this(code, null, code.getMessage(), cause); }
    public PaymentException(PaymentErrorCode code, String message, Throwable cause) { this(code, null, message, cause); }
    public PaymentException(PaymentErrorCode code, String detailCode, String message) { this(code, detailCode, message, null); }

    private PaymentException(PaymentErrorCode code, String detailCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = code;
        this.detailCode = detailCode;
    }
}
