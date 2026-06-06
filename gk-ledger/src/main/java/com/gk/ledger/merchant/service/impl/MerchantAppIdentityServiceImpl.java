package com.gk.ledger.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.ledger.merchant.dao.MerchantAppDao;
import com.gk.ledger.merchant.dto.MerchantAppIdentity;
import com.gk.ledger.merchant.entity.MerchantAppEntity;
import com.gk.ledger.merchant.service.MerchantAppIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantAppIdentityServiceImpl implements MerchantAppIdentityService {
    private final MerchantAppDao merchantAppDao;

    @Override
    public MerchantAppIdentity getEnabledIdentity(String appId) {
        MerchantAppEntity app = merchantAppDao.selectOne(new QueryWrapper<MerchantAppEntity>()
                .eq("app_id", appId)
                .eq("status", 1)
                .last("LIMIT 1"));
        if (app == null) {
            throw new IllegalArgumentException("Merchant app is disabled or does not exist");
        }
        return new MerchantAppIdentity(
                app.getTenantId(),
                app.getMerchantId(),
                app.getAppId(),
                app.getApiSecret(),
                app.getNotifyUrl(),
                app.getIpWhitelist()
        );
    }
}
