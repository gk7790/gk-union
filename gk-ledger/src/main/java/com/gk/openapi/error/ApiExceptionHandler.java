package com.gk.openapi.error;

import com.gk.infra.telegram.alert.TgAlertService;
import com.gk.openapi.security.ApiReqContext;
import com.gk.openapi.security.ApiReqContextHolder;
import com.gk.openapi.tools.ApiR;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.gk.openapi")
public class ApiExceptionHandler {
    private final ObjectProvider<TgAlertService> tgAlertServiceProvider;

    @ExceptionHandler(ApiException.class)
    public ApiR<?> handleOpenApiException(ApiException ex) {
        return ApiR.error(
                ex.getErrorCode().name(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiR<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
                .orElse("Invalid request");
        return ApiR.error(
                ApiErrorCode.INVALID_REQUEST.name(),
                message
        );
    }

    @ExceptionHandler(Exception.class)
    public ApiR<?> handleException(Exception ex, HttpServletRequest request) {
        log.error("OpenAPI request failed: {}", ex.getMessage(), ex);
        sendSystemErrorAlert(ex, request);
        return ApiR.error(ApiErrorCode.SYSTEM_ERROR);
    }

    /**
     * OpenAPI 兜底异常代表系统异常，推送到 Telegram 平台告警群。
     */
    private void sendSystemErrorAlert(Exception ex, HttpServletRequest request) {
        TgAlertService tgAlertService = tgAlertServiceProvider.getIfAvailable();
        if (tgAlertService == null) {
            return;
        }

        ApiReqContext context = ApiReqContextHolder.get();
        try {
            tgAlertService.systemError(getTenantId(context), getMerchantId(context),
                    "OpenAPI系统异常", buildAlertContent(context, ex, request), getTraceId(context));
        } catch (Exception alertEx) {
            log.warn("send OpenAPI Telegram system error alert failed: {}", alertEx.getMessage());
        }
    }

    /**
     * 告警内容只保留定位信息，不输出请求体、签名或密钥类敏感字段。
     */
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
     * 组装 OpenAPI 请求入口，便于从告警反查具体接口。
     */
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
