package com.gk.payment.psp;

import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.request.PspOrderRequest;

/**
 * Converts payment orders to PSP boundary requests.
 */
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
