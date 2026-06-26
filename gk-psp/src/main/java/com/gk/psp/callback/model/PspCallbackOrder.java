package com.gk.psp.callback.model;


import java.math.BigDecimal;

public record PspCallbackOrder(
        Long id,
        Long tenantId,
        Long merchantId,
        String merchantNo,
        Long merchantAppId,
        String appId,
        Long pspId,
        String pspCode,
        Long pspAccountId,
        String apiSecret,
        String orderNo,
        String merchantOrderNo,
        String pspOrderNo,
        String status,
        BigDecimal amount,
        BigDecimal merchantFeeAmount,
        BigDecimal settleAmount,
        BigDecimal totalDebitAmount,
        String currency,
        String notifyUrl
) {
}
