package com.gk.psp.route.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PspRouteSelectorImpl implements PspRouteSelector {
    private static final int STATUS_ENABLED = 1;

    private final PspRouteRuleDao pspRouteRuleDao;
    private final PspProviderDao pspProviderDao;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;

    @Override
    public PspRouteResult selectPayin(PayOrderEntity order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    @Override
    public PspRouteResult selectPayout(PayoutOrderEntity order) {
        return select(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYOUT.code()
        );
    }

    private PspRouteResult select(
            Long tenantId,
            Long merchantId,
            Long merchantAppId,
            String countryCode,
            String currency,
            String methodCode,
            java.math.BigDecimal amount,
            String direction
    ) {
        List<PspRouteRuleEntity> rules = pspRouteRuleDao.selectList(
                new QueryWrapper<PspRouteRuleEntity>()
                        .eq("tenant_id", tenantId)
                        .eq("country_code", countryCode)
                        .eq("currency", currency)
                        .eq("method_code", methodCode)
                        .eq("direction", direction)
                        .eq("status", STATUS_ENABLED)
                        .and(wrapper -> wrapper.isNull("merchant_id").or().eq("merchant_id", merchantId))
                        .and(wrapper -> wrapper.isNull("merchant_app_id").or().eq("merchant_app_id", merchantAppId))
                        .and(wrapper -> wrapper.isNull("min_amount").or().le("min_amount", amount))
                        .and(wrapper -> wrapper.isNull("max_amount").or().ge("max_amount", amount))
                        .orderByAsc("priority", "id")
        );

        PspRouteRuleEntity rule = rules.stream()
                .filter(this::matchesTimeWindow)
                .min(Comparator.comparingInt((PspRouteRuleEntity item) -> scopeScore(item, merchantId, merchantAppId))
                        .thenComparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority())
                        .thenComparing(PspRouteRuleEntity::getId))
                .orElseThrow(() -> new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "No available PSP route"));

        PspProviderEntity provider = requireProvider(rule.getPspId(), direction);
        PspMethodEntity method = requireMethod(rule.getPspMethodId());
        PspAccountEntity account = requirePspAccount(rule.getPspAccountId());
        PspRouteResult result = new PspRouteResult();
        result.setRouteRuleId(rule.getId());
        result.setPspId(provider.getId());
        result.setPspCode(provider.getPspCode());
        result.setPspBaseUrl(provider.getBaseUrl());
        result.setProviderConfigJson(provider.getConfigJson());
        result.setPspMethodId(method.getId());
        result.setPspMethodCode(method.getPspMethodCode());
        result.setMethodConfigJson(method.getConfigJson());
        result.setPspAccountId(account.getId());
        result.setPspAccountNo(account.getPspAccountNo());
        result.setPspAccountApiKey(account.getApiKey());
        result.setPspAccountApiSecret(account.getApiSecret());
        result.setAccountConfigJson(account.getConfigJson());
        return result;
    }

    private PspProviderEntity requireProvider(Long pspId, String direction) {
        PspProviderEntity provider = pspProviderDao.selectById(pspId);
        if (provider == null || !Integer.valueOf(STATUS_ENABLED).equals(provider.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP provider is not available");
        }
        boolean supported = PayDirectionEnum.PAYOUT.code().equals(direction)
                ? Integer.valueOf(1).equals(provider.getSupportPayout())
                : Integer.valueOf(1).equals(provider.getSupportPayin());
        if (!supported) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP provider is not available");
        }
        return provider;
    }

    private PspMethodEntity requireMethod(Long pspMethodId) {
        PspMethodEntity method = pspMethodDao.selectById(pspMethodId);
        if (method == null || !Integer.valueOf(STATUS_ENABLED).equals(method.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP method is not available");
        }
        return method;
    }

    private PspAccountEntity requirePspAccount(Long pspAccountId) {
        PspAccountEntity account = pspAccountDao.selectById(pspAccountId);
        if (account == null || !Integer.valueOf(STATUS_ENABLED).equals(account.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP account is not available");
        }
        return account;
    }

    private boolean matchesTimeWindow(PspRouteRuleEntity rule) {
        LocalTime start = rule.getStartTime();
        LocalTime end = rule.getEndTime();
        if (start == null || end == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private int scopeScore(PspRouteRuleEntity rule, Long merchantId, Long merchantAppId) {
        boolean merchantMatched = Objects.equals(rule.getMerchantId(), merchantId);
        boolean appMatched = Objects.equals(rule.getMerchantAppId(), merchantAppId);
        if (merchantMatched && appMatched) {
            return 0;
        }
        if (merchantMatched && rule.getMerchantAppId() == null) {
            return 10;
        }
        if (rule.getMerchantId() == null && rule.getMerchantAppId() == null) {
            return 20;
        }
        return 100;
    }
}
