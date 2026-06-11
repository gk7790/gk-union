package com.gk.psp.callback.model;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;

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
    public static PspCallbackOrder of(PayOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(),
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantNo(),
                order.getMerchantAppId(),
                order.getAppId(),
                order.getPspId(),
                order.getPspCode(),
                order.getPspAccountId(),
                apiSecret,
                order.getPayOrderNo(),
                order.getMerchantOrderNo(),
                order.getPspOrderNo(),
                order.getStatus(),
                order.getAmount(),
                order.getMerchantFeeAmount(),
                order.getSettleAmount(),
                null,
                order.getCurrency(),
                order.getNotifyUrl()
        );
    }

    public static PspCallbackOrder of(PayoutOrderEntity order, String apiSecret) {
        return new PspCallbackOrder(
                order.getId(),
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantNo(),
                order.getMerchantAppId(),
                order.getAppId(),
                order.getPspId(),
                order.getPspCode(),
                order.getPspAccountId(),
                apiSecret,
                order.getPayoutOrderNo(),
                order.getMerchantOrderNo(),
                order.getPspOrderNo(),
                order.getStatus(),
                order.getAmount(),
                order.getMerchantFeeAmount(),
                null,
                order.getTotalDebitAmount(),
                order.getCurrency(),
                order.getNotifyUrl()
        );
    }
}
