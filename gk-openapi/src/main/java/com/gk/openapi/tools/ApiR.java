package com.gk.openapi.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gk.openapi.error.ApiErrorCode;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiR<T> {
    private String code;
    private String message;
    private long timestamp;
    private T data;

    public static <T> ApiR<T> success(T data) {
        ApiR<T> response = new ApiR<>();
        response.setCode("SUCCESS");
        response.setMessage("success");
        response.setTimestamp(Instant.now().toEpochMilli());
        response.setData(data);
        return response;
    }

    public static <T> ApiR<T> error(String code, String message) {
        ApiR<T> response = new ApiR<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }

    public static <T> ApiR<T> error(ApiErrorCode apiErrorCode) {
        ApiR<T> response = new ApiR<>();
        response.setCode(apiErrorCode.name());
        response.setMessage(apiErrorCode.getMessage());
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }
}
