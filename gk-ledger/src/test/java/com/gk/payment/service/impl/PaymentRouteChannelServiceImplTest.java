package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dto.PaymentRouteChannelOptionsResponse;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentRouteChannelServiceImplTest {

    @Test
    void optionsReturnsCascadeDataForRouteGroup() {
        PaymentRouteChannelDao routeChannelDao = mock(PaymentRouteChannelDao.class);
        PaymentRouteGroupDao routeGroupDao = mock(PaymentRouteGroupDao.class);
        PspProviderDao pspProviderDao = mock(PspProviderDao.class);
        PspMethodDao pspMethodDao = mock(PspMethodDao.class);
        PspAccountDao pspAccountDao = mock(PspAccountDao.class);
        PspFeeRuleDao pspFeeRuleDao = mock(PspFeeRuleDao.class);
        LedgerAccountService ledgerAccountService = mock(LedgerAccountService.class);
        PayinPlanCache payinPlanCache = mock(PayinPlanCache.class);
        PaymentPlanCacheService paymentPlanCacheService = mock(PaymentPlanCacheService.class);
        PaymentRouteChannelServiceImpl service = new PaymentRouteChannelServiceImpl(
                routeGroupDao,
                pspProviderDao,
                pspMethodDao,
                pspAccountDao,
                pspFeeRuleDao,
                ledgerAccountService,
                payinPlanCache,
                paymentPlanCacheService
        );
        service.setBaseDao(routeChannelDao);

        when(routeGroupDao.selectById(100L)).thenReturn(routeGroup());
        when(pspMethodDao.selectList(anyPspMethodWrapper())).thenReturn(List.of(pspMethod()));
        when(pspProviderDao.selectList(anyPspProviderWrapper())).thenReturn(List.of(pspProvider()));
        when(pspAccountDao.selectList(anyPspAccountWrapper())).thenReturn(List.of(pspAccount()));
        when(pspFeeRuleDao.selectList(anyPspFeeRuleWrapper())).thenReturn(List.of(pspFeeRule()));

        DynMap params = DynMap.empty();
        params.put("groupId", 100L);

        PaymentRouteChannelOptionsResponse response = service.options(params);

        assertNotNull(response.getRouteGroup());
        assertEquals(1, response.getPspProviders().size());
        assertEquals(1, response.getPspMethods().size());
        assertEquals(1, response.getPspAccounts().size());
        assertEquals(1, response.getPspFeeRules().size());
        assertEquals(200L, response.getPspProviders().getFirst().getValue());
        assertEquals(300L, response.getPspMethods().getFirst().getValue());
        assertEquals(400L, response.getPspAccounts().getFirst().getValue());
        assertEquals(500L, response.getPspFeeRules().getFirst().getValue());
    }

    @Test
    void optionsCanUseUnsavedGroupDimensions() {
        PaymentRouteChannelServiceImpl service = newService(
                mock(PaymentRouteGroupDao.class),
                List.of(pspProvider()),
                List.of(pspMethod()),
                List.of(pspAccount()),
                List.of(pspFeeRule())
        );
        DynMap params = DynMap.empty();
        params.put("tenantId", 1L);
        params.put("direction", "PAYIN");
        params.put("countryCode", "");
        params.put("currency", "PHP");
        params.put("methodCode", "GCASH");

        PaymentRouteChannelOptionsResponse response = service.options(params);

        assertNotNull(response.getRouteGroup());
        assertEquals(1L, response.getRouteGroup().getTenantId());
        assertEquals("PAYIN", response.getRouteGroup().getDirection());
        assertEquals(1, response.getPspProviders().size());
        assertEquals(1, response.getPspMethods().size());
        assertEquals(1, response.getPspAccounts().size());
        assertEquals(1, response.getPspFeeRules().size());
    }

    private PaymentRouteChannelServiceImpl newService(PaymentRouteGroupDao routeGroupDao,
                                                      List<PspProviderEntity> providers,
                                                      List<PspMethodEntity> methods,
                                                      List<PspAccountEntity> accounts,
                                                      List<PspFeeRuleEntity> feeRules) {
        PaymentRouteChannelDao routeChannelDao = mock(PaymentRouteChannelDao.class);
        PspProviderDao pspProviderDao = mock(PspProviderDao.class);
        PspMethodDao pspMethodDao = mock(PspMethodDao.class);
        PspAccountDao pspAccountDao = mock(PspAccountDao.class);
        PspFeeRuleDao pspFeeRuleDao = mock(PspFeeRuleDao.class);
        PaymentRouteChannelServiceImpl service = new PaymentRouteChannelServiceImpl(
                routeGroupDao,
                pspProviderDao,
                pspMethodDao,
                pspAccountDao,
                pspFeeRuleDao,
                mock(LedgerAccountService.class),
                mock(PayinPlanCache.class),
                mock(PaymentPlanCacheService.class)
        );
        service.setBaseDao(routeChannelDao);
        when(pspMethodDao.selectList(anyPspMethodWrapper())).thenReturn(methods);
        when(pspProviderDao.selectList(anyPspProviderWrapper())).thenReturn(providers);
        when(pspAccountDao.selectList(anyPspAccountWrapper())).thenReturn(accounts);
        when(pspFeeRuleDao.selectList(anyPspFeeRuleWrapper())).thenReturn(feeRules);
        return service;
    }

    private QueryWrapper<PspMethodEntity> anyPspMethodWrapper() {
        return any();
    }

    private QueryWrapper<PspProviderEntity> anyPspProviderWrapper() {
        return any();
    }

    private QueryWrapper<PspAccountEntity> anyPspAccountWrapper() {
        return any();
    }

    private QueryWrapper<PspFeeRuleEntity> anyPspFeeRuleWrapper() {
        return any();
    }

    private PaymentRouteGroupEntity routeGroup() {
        PaymentRouteGroupEntity group = new PaymentRouteGroupEntity();
        group.setId(100L);
        group.setTenantId(1L);
        group.setGroupCode("PH_GCASH_PAYIN");
        group.setGroupName("PH GCASH PAYIN");
        group.setDirection("PAYIN");
        group.setCountryCode("");
        group.setCurrency("PHP");
        group.setMethodCode("GCASH");
        group.setStrategy("PRIORITY_WEIGHT");
        group.setStatus(StatusEnum.NORMAL.code());
        return group;
    }

    private PspProviderEntity pspProvider() {
        PspProviderEntity provider = new PspProviderEntity();
        provider.setId(200L);
        provider.setPspCode("XPAY");
        provider.setPspName("XPay");
        provider.setCountryCode("PH");
        provider.setSupportPayin(1);
        provider.setSupportPayout(1);
        provider.setStatus(StatusEnum.NORMAL.code());
        return provider;
    }

    private PspMethodEntity pspMethod() {
        PspMethodEntity method = new PspMethodEntity();
        method.setId(300L);
        method.setPspId(200L);
        method.setPspCode("XPAY");
        method.setMethodCode("GCASH");
        method.setPspMethodCode("X_GCASH");
        method.setMethodName("XPay GCASH");
        method.setCountryCode("");
        method.setCurrency("PHP");
        method.setDirection("PAYIN");
        method.setStatus(StatusEnum.NORMAL.code());
        return method;
    }

    private PspAccountEntity pspAccount() {
        PspAccountEntity account = new PspAccountEntity();
        account.setId(400L);
        account.setTenantId(1L);
        account.setPspId(200L);
        account.setPspAccountNo("XPAY-PHP-001");
        account.setPspAccountName("XPay PHP");
        account.setStatus(StatusEnum.NORMAL.code());
        return account;
    }

    private PspFeeRuleEntity pspFeeRule() {
        PspFeeRuleEntity rule = new PspFeeRuleEntity();
        rule.setId(500L);
        rule.setTenantId(1L);
        rule.setPspId(200L);
        rule.setPspMethodId(300L);
        rule.setPspAccountId(400L);
        rule.setRuleName("XPay GCASH Cost");
        rule.setDirection("PAYIN");
        rule.setCountryCode("");
        rule.setCurrency("PHP");
        rule.setMethodCode("GCASH");
        rule.setFeeMode("RATE_FIXED");
        rule.setFeeRate(new BigDecimal("0.01"));
        rule.setFeeFixed(new BigDecimal("2"));
        rule.setPriority(1);
        rule.setStatus(StatusEnum.NORMAL.code());
        return rule;
    }
}
