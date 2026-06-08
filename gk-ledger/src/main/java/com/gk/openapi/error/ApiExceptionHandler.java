package com.gk.openapi.error;

import com.gk.openapi.tools.ApiR;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.gk.openapi")
public class ApiExceptionHandler {

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
    public ApiR<?> handleException(Exception ex) {
        log.error("OpenAPI request failed: {}", ex.getMessage(), ex);
        return ApiR.error(ApiErrorCode.SYSTEM_ERROR);
    }
}
