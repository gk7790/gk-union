package com.gk.openapi.error;

public record ApiErrorDescriptor(
        ApiErrorCode code,
        ApiErrorCategory category,
        String publicMessage,
        boolean retryable
) {
}
