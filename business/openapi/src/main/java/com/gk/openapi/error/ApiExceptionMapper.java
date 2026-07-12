package com.gk.openapi.error;

import com.gk.payment.domain.error.PaymentErrorCode;
import com.gk.payment.domain.error.PaymentException;

public final class ApiExceptionMapper {
    private ApiExceptionMapper() {
    }

    public static ApiErrorDescriptor resolve(Throwable ex) {
        if (ex instanceof ApiException apiException) {
            return sanitize(apiException);
        }
        if (ex instanceof PaymentException paymentException) {
            ApiErrorCode code = mapPaymentCode(paymentException.getErrorCode());
            return descriptor(code, category(code), safeMessage(code, paymentException.getMessage()), retryable(code));
        }

        KnownError knownError = KnownError.from(ex);
        if (knownError != null) {
            return knownError.descriptor();
        }

        if (ex instanceof IllegalArgumentException) {
            return descriptor(ApiErrorCode.INVALID_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.INVALID_REQUEST, false);
        }
        if (ex instanceof IllegalStateException) {
            return descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true);
        }
        return descriptor(ApiErrorCode.SYSTEM_ERROR, ApiErrorCategory.SYSTEM, ApiErrorMessage.SYSTEM_ERROR, true);
    }

    public static ApiException toApiException(Throwable ex) {
        if (ex instanceof ApiException apiException) {
            ApiErrorDescriptor descriptor = sanitize(apiException);
            if (descriptor.code() == apiException.getErrorCode()
                    && descriptor.publicMessage().equals(apiException.getMessage())) {
                return apiException;
            }
            return new ApiException(descriptor.code(), descriptor.publicMessage(), apiException);
        }
        ApiErrorDescriptor descriptor = resolve(ex);
        return new ApiException(descriptor.code(), descriptor.publicMessage(), ex);
    }

    private static ApiErrorDescriptor sanitize(ApiException ex) {
        KnownError knownError = KnownError.from(ex);
        if (knownError != null) {
            return knownError.descriptor();
        }
        ApiErrorCode code = ex.getErrorCode() == null ? ApiErrorCode.SYSTEM_ERROR : ex.getErrorCode();
        return descriptor(code, category(code), safeMessage(code, ex.getMessage()), retryable(code));
    }

    private static ApiErrorDescriptor descriptor(ApiErrorCode code,
                                                ApiErrorCategory category,
                                                String publicMessage,
                                                boolean retryable) {
        return new ApiErrorDescriptor(code, category, publicMessage, retryable);
    }

    private static ApiErrorCategory category(ApiErrorCode code) {
        if (code == null) {
            return ApiErrorCategory.SYSTEM;
        }
        return switch (code) {
            case INVALID_APP, APP_DISABLED, MERCHANT_DISABLED, INVALID_IP, INVALID_TIMESTAMP,
                 REPLAY_REQUEST, INVALID_SIGNATURE, UNSUPPORTED_SIGN_TYPE -> ApiErrorCategory.AUTH;
            case INVALID_REQUEST, DUPLICATE_REQUEST -> ApiErrorCategory.REQUEST;
            case INVALID_AMOUNT, UNSUPPORTED_METHOD, ORDER_NOT_FOUND, ORDER_STATUS_INVALID -> ApiErrorCategory.BUSINESS;
            case INSUFFICIENT_BALANCE -> ApiErrorCategory.LEDGER;
            case SERVICE_NOT_READY -> ApiErrorCategory.CONFIGURATION;
            case SYSTEM_ERROR -> ApiErrorCategory.SYSTEM;
            default -> ApiErrorCategory.SYSTEM;
        };
    }

    private static ApiErrorCode mapPaymentCode(PaymentErrorCode code) {
        if (code == null) return ApiErrorCode.SYSTEM_ERROR;
        return switch (code) {
            case INVALID_REQUEST -> ApiErrorCode.INVALID_REQUEST;
            case INVALID_AMOUNT -> ApiErrorCode.INVALID_AMOUNT;
            case UNSUPPORTED_METHOD -> ApiErrorCode.UNSUPPORTED_METHOD;
            case INSUFFICIENT_BALANCE -> ApiErrorCode.INSUFFICIENT_BALANCE;
            case ORDER_NOT_FOUND -> ApiErrorCode.ORDER_NOT_FOUND;
            case ORDER_STATUS_INVALID -> ApiErrorCode.ORDER_STATUS_INVALID;
            case SERVICE_NOT_READY -> ApiErrorCode.SERVICE_NOT_READY;
            case SYSTEM_ERROR -> ApiErrorCode.SYSTEM_ERROR;
        };
    }

    private static boolean retryable(ApiErrorCode code) {
        return code == ApiErrorCode.SERVICE_NOT_READY || code == ApiErrorCode.SYSTEM_ERROR;
    }

    private static String safeMessage(ApiErrorCode code, String message) {
        if (isBlank(message)) {
            return code.getMessage();
        }
        return switch (code) {
            case DUPLICATE_REQUEST -> ApiErrorMessage.DUPLICATE_REQUEST;
            case INSUFFICIENT_BALANCE -> ApiErrorMessage.INSUFFICIENT_BALANCE;
            case SERVICE_NOT_READY -> ApiErrorMessage.SERVICE_NOT_READY;
            case SYSTEM_ERROR -> ApiErrorMessage.SYSTEM_ERROR;
            default -> message;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean matchesClass(Throwable ex, String className) {
        return ex != null && className.equals(ex.getClass().getName());
    }

    private enum KnownError {
        PAYMENT_PLAN_BUCKET_NOT_CONFIGURED(
                "Payment plan bucket is not configured",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        PAYMENT_PLAN_BUCKET_AMOUNT_MISMATCH(
                "Payment plan bucket does not match amount",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_AMOUNT, ApiErrorCategory.BUSINESS, ApiErrorMessage.AMOUNT_NOT_SUPPORTED, false)
        ),
        PAYMENT_PLAN_BUCKET_OVERLAPPED(
                "Payment plan bucket is overlapped",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        PAYMENT_PLAN_ROUTE_OPTION_UNAVAILABLE(
                "Payment plan route option is not available",
                ApiExceptionMapper.descriptor(ApiErrorCode.UNSUPPORTED_METHOD, ApiErrorCategory.BUSINESS, ApiErrorMessage.METHOD_NOT_SUPPORTED, false)
        ),
        ACTIVE_PAYMENT_PLAN_NOT_PUBLISHED(
                "ACTIVE payment plan is not published",
                ApiExceptionMapper.descriptor(ApiErrorCode.UNSUPPORTED_METHOD, ApiErrorCategory.BUSINESS, ApiErrorMessage.METHOD_NOT_SUPPORTED, false)
        ),
        PSP_PROVIDER_UNAVAILABLE(
                "PSP provider is not available",
                ApiExceptionMapper.descriptor(ApiErrorCode.UNSUPPORTED_METHOD, ApiErrorCategory.UPSTREAM, ApiErrorMessage.METHOD_NOT_SUPPORTED, false)
        ),
        PSP_METHOD_UNAVAILABLE(
                "PSP method is not available",
                ApiExceptionMapper.descriptor(ApiErrorCode.UNSUPPORTED_METHOD, ApiErrorCategory.UPSTREAM, ApiErrorMessage.METHOD_NOT_SUPPORTED, false)
        ),
        PSP_ACCOUNT_UNAVAILABLE(
                "PSP account is not available",
                ApiExceptionMapper.descriptor(ApiErrorCode.UNSUPPORTED_METHOD, ApiErrorCategory.UPSTREAM, ApiErrorMessage.METHOD_NOT_SUPPORTED, false)
        ),
        PSP_ROUTE_SNAPSHOT_INCOMPLETE(
                "PSP route snapshot is incomplete",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        PSP_ROUTE_SNAPSHOT_UNAVAILABLE(
                "PSP route snapshot is unavailable",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        MERCHANT_FEE_RULE_NOT_CONFIGURED(
                "Merchant fee rule is not configured",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        PSP_FEE_RULE_NOT_CONFIGURED(
                "PSP fee rule is not configured",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        PSP_FEE_MODE_INVALID(
                "Invalid PSP fee mode",
                ApiExceptionMapper.descriptor(ApiErrorCode.SERVICE_NOT_READY, ApiErrorCategory.CONFIGURATION, ApiErrorMessage.SERVICE_NOT_READY, true)
        ),
        AMOUNT_NOT_POSITIVE(
                "amount must be greater than zero",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_AMOUNT, ApiErrorCategory.BUSINESS, ApiErrorMessage.INVALID_AMOUNT, false)
        ),
        MERCHANT_FEE_EXCEEDS_AMOUNT(
                "Merchant fee cannot exceed order amount",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_AMOUNT, ApiErrorCategory.BUSINESS, ApiErrorMessage.INVALID_AMOUNT, false)
        ),
        DUPLICATE_KEY(
                "org.springframework.dao.DuplicateKeyException",
                ApiExceptionMapper.descriptor(ApiErrorCode.DUPLICATE_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.DUPLICATE_REQUEST, false),
                true
        ),
        INSUFFICIENT_BALANCE(
                "com.gk.ledger.exception.InsufficientLedgerBalanceException",
                ApiExceptionMapper.descriptor(ApiErrorCode.INSUFFICIENT_BALANCE, ApiErrorCategory.LEDGER, ApiErrorMessage.INSUFFICIENT_BALANCE, false),
                true
        ),
        CONSTRAINT_VIOLATION(
                "jakarta.validation.ConstraintViolationException",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.INVALID_REQUEST, false),
                true
        ),
        MESSAGE_NOT_READABLE(
                "org.springframework.http.converter.HttpMessageNotReadableException",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.INVALID_REQUEST, false),
                true
        ),
        MISSING_REQUEST_PARAMETER(
                "org.springframework.web.bind.MissingServletRequestParameterException",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.INVALID_REQUEST, false),
                true
        ),
        ARGUMENT_TYPE_MISMATCH(
                "org.springframework.web.method.annotation.MethodArgumentTypeMismatchException",
                ApiExceptionMapper.descriptor(ApiErrorCode.INVALID_REQUEST, ApiErrorCategory.REQUEST, ApiErrorMessage.INVALID_REQUEST, false),
                true
        );

        private final String matchValue;
        private final ApiErrorDescriptor descriptor;
        private final boolean matchClassName;

        KnownError(String matchValue, ApiErrorDescriptor descriptor) {
            this(matchValue, descriptor, false);
        }

        KnownError(String matchValue, ApiErrorDescriptor descriptor, boolean matchClassName) {
            this.matchValue = matchValue;
            this.descriptor = descriptor;
            this.matchClassName = matchClassName;
        }

        private ApiErrorDescriptor descriptor() {
            return descriptor;
        }

        private static KnownError from(Throwable ex) {
            if (ex == null) {
                return null;
            }
            for (KnownError item : values()) {
                if (item.matches(ex)) {
                    return item;
                }
            }
            return null;
        }

        private boolean matches(Throwable ex) {
            if (matchClassName) {
                return matchesClass(ex, matchValue);
            }
            return matchValue.equals(ex.getMessage());
        }
    }
}
