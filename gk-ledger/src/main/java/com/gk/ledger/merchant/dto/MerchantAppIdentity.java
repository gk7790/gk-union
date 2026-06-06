package com.gk.ledger.merchant.dto;

public record MerchantAppIdentity(
        Long tenantId,
        Long merchantId,
        String appId,
        String apiSecret,
        String notifyUrl,
        String ipWhitelist
) {
}
