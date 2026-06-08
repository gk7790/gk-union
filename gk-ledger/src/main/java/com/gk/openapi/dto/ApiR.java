package com.gk.openapi.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiR<T> {
    private String code;
    private String message;
    private String requestId;
    private long timestamp;
    private T data;

    public static <T> ApiR<T> success(T data) {
        return success(data, null);
    }

    public static <T> ApiR<T> success(T data, String requestId) {
        ApiR<T> response = new ApiR<>();
        response.setCode("SUCCESS");
        response.setMessage("success");
        response.setRequestId(requestId);
        response.setTimestamp(Instant.now().toEpochMilli());
        response.setData(data);
        return response;
    }

    public static <T> ApiR<T> error(String code, String message, String requestId) {
        ApiR<T> response = new ApiR<>();
        response.setCode(code);
        response.setMessage(message);
        response.setRequestId(requestId);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }
}
