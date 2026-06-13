package com.gk.psp.query.impl;

import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PspOrderRouteSnapshot {
    private final PspProviderDao pspProviderDao;
    private final PspAccountDao pspAccountDao;

    PspRouteResult fromPayOrder(PayOrderEntity order) {
        PspRouteResult route = base(order.getPspId(), order.getPspAccountId());
        route.setRouteRuleId(order.getRouteRuleId());
        route.setPspCode(order.getPspCode());
        route.setPspMethodId(order.getPspMethodId());
        route.setPspMethodCode(order.getPspMethodCode());
        route.setPspAccountNo(order.getPspAccountNo());
        return route;
    }

    PspRouteResult fromPayoutOrder(PayoutOrderEntity order) {
        PspRouteResult route = base(order.getPspId(), order.getPspAccountId());
        route.setRouteRuleId(order.getRouteRuleId());
        route.setPspCode(order.getPspCode());
        route.setPspMethodId(order.getPspMethodId());
        route.setPspMethodCode(order.getPspMethodCode());
        route.setPspAccountNo(order.getPspAccountNo());
        return route;
    }

    private PspRouteResult base(Long pspId, Long pspAccountId) {
        if (pspId == null || pspAccountId == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "PSP route snapshot is incomplete");
        }
        PspProviderEntity provider = pspProviderDao.selectById(pspId);
        PspAccountEntity account = pspAccountDao.selectById(pspAccountId);
        if (provider == null || account == null) {
            throw new ApiException(ApiErrorCode.SERVICE_NOT_READY, "PSP route snapshot is unavailable");
        }
        PspRouteResult route = new PspRouteResult();
        route.setPspId(pspId);
        route.setPspCode(provider.getPspCode());
        route.setPspBaseUrl(provider.getBaseUrl());
        route.setProviderConfigJson(provider.getConfigJson());
        route.setPspAccountId(pspAccountId);
        route.setPspAccountNo(account.getPspAccountNo());
        route.setPspAccountApiKey(account.getApiKey());
        route.setPspAccountApiSecret(account.getApiSecret());
        route.setAccountConfigJson(account.getConfigJson());
        return route;
    }
}
