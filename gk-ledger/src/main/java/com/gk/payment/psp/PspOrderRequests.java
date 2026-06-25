package com.gk.payment.psp;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.request.PspOrderRequest;

/**
 * payment 订单实体�?PSP 边界请求的转换器�? * <p>
 * 转换逻辑留在 payment 侧，PSP 模块只接收自己的请求对象，不感知支付订单实体�? */
public final class PspOrderRequests {
    private PspOrderRequests() {
    }

    public static PspOrderRequest fromPayOrder(PayOrderEntity order) {
        return PspOrderRequest.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .merchantId(order.getMerchantId())
                .merchantAppId(order.getMerchantAppId())
                .orderNo(order.getPayOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .countryCode(order.getCountryCode())
                .currency(order.getCurrency())
                .methodCode(order.getMethodCode())
                .amount(order.getAmount())
                .returnUrl(order.getReturnUrl())
                .payerJson(order.getPayerJson())
                .extraJson(order.getExtraJson())
                .routeRuleId(order.getRouteRuleId())
                .pspId(order.getPspId())
                .pspCode(order.getPspCode())
                .pspMethodId(order.getPspMethodId())
                .pspMethodCode(order.getPspMethodCode())
                .pspAccountId(order.getPspAccountId())
                .pspAccountNo(order.getPspAccountNo())
                .pspOrderNo(order.getPspOrderNo())
                .build();
    }

    public static PspOrderRequest fromPayoutOrder(PayoutOrderEntity order) {
        return PspOrderRequest.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .merchantId(order.getMerchantId())
                .merchantAppId(order.getMerchantAppId())
                .orderNo(order.getPayoutOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .countryCode(order.getCountryCode())
                .currency(order.getCurrency())
                .methodCode(order.getMethodCode())
                .amount(order.getAmount())
                .payeeName(order.getPayeeName())
                .payeeAccountNo(order.getPayeeAccountNo())
                .payeeBankCode(order.getPayeeBankCode())
                .payeeWalletType(order.getPayeeWalletType())
                .payeeJson(order.getPayeeJson())
                .extraJson(order.getExtraJson())
                .routeRuleId(order.getRouteRuleId())
                .pspId(order.getPspId())
                .pspCode(order.getPspCode())
                .pspMethodId(order.getPspMethodId())
                .pspMethodCode(order.getPspMethodCode())
                .pspAccountId(order.getPspAccountId())
                .pspAccountNo(order.getPspAccountNo())
                .pspOrderNo(order.getPspOrderNo())
                .build();
    }
}
