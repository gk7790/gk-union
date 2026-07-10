package com.gk.openapi.error;

import com.gk.common.tools.StringFormat;
import com.gk.infra.telegram.TgAlertService;
import com.gk.ledger.exception.InsufficientLedgerBalanceException;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.tools.ApiR;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.gk.openapi")
public class ApiExceptionHandler {
    private final ObjectProvider<TgAlertService> tgAlertServiceProvider;

    @ExceptionHandler(ApiException.class)
    public ApiR<?> handleOpenApiException(ApiException ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        if (descriptor.code() == ApiErrorCode.SYSTEM_ERROR) {
            log.error("OpenAPI system error: {}", ex.getMessage(), ex);
        }
        return error(descriptor);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiR<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> StringUtils.defaultIfBlank(error.getDefaultMessage(), "Invalid request"))
                .orElse("Invalid request");
        return error(ApiErrorCode.INVALID_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiR<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.warn("OpenAPI invalid argument: {}", descriptor.publicMessage());
        return error(descriptor);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiR<?> handleIllegalStateException(IllegalStateException ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.warn("OpenAPI service not ready: {}", descriptor.publicMessage());
        return error(descriptor);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ApiR<?> handleInvalidRequestException(Exception ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.warn("OpenAPI invalid request: {}", descriptor.publicMessage());
        return error(descriptor);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ApiR<?> handleDuplicateKeyException(DuplicateKeyException ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.warn("OpenAPI duplicate request: {}", ex.getMessage());
        return error(descriptor);
    }

    @ExceptionHandler(InsufficientLedgerBalanceException.class)
    public ApiR<?> handleInsufficientBalanceException(InsufficientLedgerBalanceException ex) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.warn("OpenAPI insufficient balance: {}", ex.getMessage());
        return error(descriptor);
    }

    @ExceptionHandler(Exception.class)
    public ApiR<?> handleException(Exception ex, HttpServletRequest request) {
        ApiErrorDescriptor descriptor = ApiExceptionMapper.resolve(ex);
        log.error("OpenAPI request failed: {}", ex.getMessage(), ex);
        sendSystemErrorAlert(ex, request);
        return error(descriptor);
    }

    private ApiR<?> error(ApiErrorCode errorCode, String message) {
        return ApiR.error(
                errorCode.name(),
                StringUtils.defaultIfBlank(message, errorCode.getMessage())
        );
    }

    private ApiR<?> error(ApiErrorDescriptor descriptor) {
        return ApiR.error(
                descriptor.code().name(),
                StringUtils.defaultIfBlank(descriptor.publicMessage(), descriptor.code().getMessage())
        );
    }

    /**
     * OpenAPI 兜底异常代表系统异常，推送到 Telegram 平台告警群     */
    private void sendSystemErrorAlert(Exception ex, HttpServletRequest request) {
        TgAlertService tgBotService = tgAlertServiceProvider.getIfAvailable();
        if (tgBotService == null) {
            return;
        }

        ApiReqContext context = ApiReqContextHolder.get();
        try {
            String text = StringFormat.format("""
                    🚨 OpenAPI系统异常 🚨
                    ──────────────
                    {}
                    """,
                    buildAlertContent(context, ex, request)
            );
            tgBotService.sysError(getTenantId(context), getMerchantId(context), text, getTraceId(context));
        } catch (Exception alertEx) {
            log.warn("send OpenAPI Telegram system error alert failed: {}", alertEx.getMessage());
        }
    }

    /**
     * 告警内容只保留定位信息，不输出请求体、签名或密钥类敏感字段     */
    private String buildAlertContent(ApiReqContext context, Exception ex, HttpServletRequest request) {
        StringBuilder content = new StringBuilder();
        appendRequestLine(content, request);
        if (context != null) {
            if (StringUtils.isNotBlank(context.getAppId())) {
                content.append("AppId: ").append(context.getAppId()).append('\n');
            }
            if (StringUtils.isNotBlank(context.getClientIp())) {
                content.append("客户端IP: ").append(context.getClientIp()).append('\n');
            }
        }
        content.append("异常: ").append(ex.getClass().getSimpleName());
        if (StringUtils.isNotBlank(ex.getMessage())) {
            content.append('\n').append("消息: ").append(ex.getMessage());
        }
        return content.toString();
    }

    /**
     * 组装 OpenAPI 请求入口，便于从告警反查具体接口     */
    private void appendRequestLine(StringBuilder content, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        content.append(StringUtils.defaultIfBlank(request.getMethod(), "-"))
                .append(' ')
                .append(StringUtils.defaultIfBlank(request.getRequestURI(), "-"))
                .append('\n');
    }

    private Long getTenantId(ApiReqContext context) {
        return context == null ? null : context.getTenantId();
    }

    private Long getMerchantId(ApiReqContext context) {
        return context == null ? null : context.getMerchantId();
    }

    private String getTraceId(ApiReqContext context) {
        return context == null ? null : context.getTraceId();
    }
}
