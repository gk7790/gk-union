package com.gk.ledger.merchant.service;

import com.gk.ledger.merchant.dto.MerchantAppIdentity;

public interface MerchantAppIdentityService {
    MerchantAppIdentity getEnabledIdentity(String appId);
}
