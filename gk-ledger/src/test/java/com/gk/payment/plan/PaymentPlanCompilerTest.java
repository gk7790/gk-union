package com.gk.payment.plan;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.payment.dao.MerchantFeeRuleDao;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dao.PaymentRouteRuleDao;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentPlanCompilerTest {

    @Test
    void requiresPaymentRouteRuleForNewProjectPlan() {
        MerchantFeeRuleDao merchantFeeRuleDao = mock(MerchantFeeRuleDao.class);
        PaymentRouteRuleDao paymentRouteRuleDao = mock(PaymentRouteRuleDao.class);
        PaymentRouteGroupDao paymentRouteGroupDao = mock(PaymentRouteGroupDao.class);
        PaymentRouteChannelDao paymentRouteChannelDao = mock(PaymentRouteChannelDao.class);
        PspFeeRuleDao pspFeeRuleDao = mock(PspFeeRuleDao.class);
        PspProviderDao pspProviderDao = mock(PspProviderDao.class);
        PspMethodDao pspMethodDao = mock(PspMethodDao.class);
        PspAccountDao pspAccountDao = mock(PspAccountDao.class);
        PspBankMappingDao pspBankMappingDao = mock(PspBankMappingDao.class);

        PaymentPlanCompiler compiler = new PaymentPlanCompiler(
                merchantFeeRuleDao,
                paymentRouteRuleDao,
                paymentRouteGroupDao,
                paymentRouteChannelDao,
                pspFeeRuleDao,
                pspProviderDao,
                pspMethodDao,
                pspAccountDao,
                pspBankMappingDao
        );

        when(merchantFeeRuleDao.selectList(any(QueryWrapper.class))).thenReturn(List.of(merchantFeeRule()));
        when(paymentRouteRuleDao.selectList(any(QueryWrapper.class))).thenReturn(List.of());
        when(pspProviderDao.selectById(100L)).thenReturn(provider());
        when(pspMethodDao.selectById(200L)).thenReturn(method());
        when(pspAccountDao.selectById(300L)).thenReturn(account());

        PaymentPlanCompileResult result = compiler.compile(request());

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> "PSP_ROUTE_RULE_MISSING".equals(error.code())));
    }

    private PaymentPlanCompileRequest request() {
        PaymentPlanCompileRequest request = new PaymentPlanCompileRequest();
        request.setTenantId(1L);
        request.setMerchantId(10L);
        request.setMerchantAppId(20L);
        request.setMerchantFeeRuleId(11L);
        request.setDirection(PayDirectionEnum.PAYIN.code());
        request.setCountryCode("");
        request.setCurrency("PHP");
        request.setMethodCode("GCASH");
        request.setMinAmount(BigDecimal.ZERO);
        request.setMaxAmount(new BigDecimal("1000.00"));
        return request;
    }

    private MerchantFeeRuleEntity merchantFeeRule() {
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setId(11L);
        rule.setTenantId(1L);
        rule.setMerchantId(10L);
        rule.setMerchantAppId(20L);
        rule.setRuleName("Merchant GCASH");
        rule.setDirection(PayDirectionEnum.PAYIN.code());
        rule.setCountryCode("");
        rule.setCurrency("PHP");
        rule.setMethodCode("GCASH");
        rule.setMinAmount(BigDecimal.ZERO);
        rule.setMaxAmount(new BigDecimal("1000.00"));
        rule.setFeeMode("RATE");
        rule.setFeeRate(new BigDecimal("0.03"));
        rule.setFeeBearer("MERCHANT");
        rule.setStatus(StatusEnum.NORMAL.code());
        return rule;
    }

    private PspProviderEntity provider() {
        PspProviderEntity provider = new PspProviderEntity();
        provider.setId(100L);
        provider.setPspCode("XPay");
        provider.setSupportPayin(1);
        provider.setStatus(StatusEnum.NORMAL.code());
        return provider;
    }

    private PspMethodEntity method() {
        PspMethodEntity method = new PspMethodEntity();
        method.setId(200L);
        method.setPspId(100L);
        method.setMethodCode("GCASH");
        method.setPspMethodCode("X_GCASH");
        method.setDirection(PayDirectionEnum.PAYIN.code());
        method.setCurrency("PHP");
        method.setStatus(StatusEnum.NORMAL.code());
        return method;
    }

    private PspAccountEntity account() {
        PspAccountEntity account = new PspAccountEntity();
        account.setId(300L);
        account.setTenantId(1L);
        account.setPspId(100L);
        account.setPspAccountNo("acct-php");
        account.setStatus(StatusEnum.NORMAL.code());
        return account;
    }
}
