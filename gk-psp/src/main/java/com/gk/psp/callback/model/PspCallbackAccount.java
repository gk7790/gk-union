package com.gk.psp.callback.model;

import lombok.Data;

@Data
public class PspCallbackAccount {
    private Long pspAccountId;
    private String pspCode;
    private String apiSecret;
}
