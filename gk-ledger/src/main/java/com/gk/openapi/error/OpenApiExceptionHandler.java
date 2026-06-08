package com.gk.openapi.error;

import com.gk.openapi.dto.ApiR;
import com.gk.openapi.security.OpenApiRequestContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.gk.openapi")
public class OpenApiExceptionHandler {

    @ExceptionHandler(OpenApiException.class)
    public ApiR<?> handleOpenApiException(OpenApiException ex) {
        return ApiR.error(
                ex.getErrorCode().name(),
                ex.getMessage(),
                OpenApiRequestContextHolder.getRequestId()
        );
    }
}
