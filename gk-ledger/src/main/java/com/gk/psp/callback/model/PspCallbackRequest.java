package com.gk.psp.callback.model;

import lombok.Data;

import java.util.Map;

@Data
public class PspCallbackRequest {
    private String pspCode;
    private String bizType;
    private String rawBody;
    private Map<String, String> headers;
    private Map<String, Object> params;
    private String clientIp;
    private String apiSecret;
}
