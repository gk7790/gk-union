package com.gk.psp.callback.model;

import lombok.Data;

@Data
public class PspCallbackAccount {
    private Long tenantId;
    private Long pspId;
    private Long pspAccountId;
    private String pspCode;
    private String apiSecret;
}
