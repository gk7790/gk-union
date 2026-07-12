package com.gk.payment.domain.error;

public final class PaymentExceptions {
    public static PaymentException normalize(Throwable exception) {
        if (exception instanceof PaymentException paymentException) return paymentException;
        if (exception instanceof IllegalArgumentException) {
            return new PaymentException(PaymentErrorCode.INVALID_REQUEST, exception.getMessage(), exception);
        }
        if (exception instanceof IllegalStateException) {
            return new PaymentException(PaymentErrorCode.SERVICE_NOT_READY, exception.getMessage(), exception);
        }
        return new PaymentException(PaymentErrorCode.SYSTEM_ERROR, exception);
    }
    private PaymentExceptions() {}
}
