package com.gk.psp.callback.model;

import com.gk.common.model.DynMap;
import lombok.Data;

@Data
public class PspCallbackRequest {
    private String pspCode;
    private String bizType;
    private String rawBody;
    private DynMap headers;
    private DynMap params;
    private String clientIp;
    private String apiSecret;
}
