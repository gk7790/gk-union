package com.gk.payment.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.infra.enums.StatusEnum;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dto.PayinConfigPrecheckRequest;
import com.gk.payment.dto.PayinConfigPrecheckResult;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.service.MerchantFeeRuleService;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import com.gk.psp.route.PspRouteSelector;
import com.gk.psp.service.PspFeeRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayinPlanServiceImplTest {
    @Mock
    private MerchantDao merchantDao;
    @Mock
    private MerchantAppDao merchantAppDao;
    @Mock
    private MerchantFeeRuleService merchantFeeRuleService;
    @Mock
    private PspRouteSelector pspRouteSelector;
    @Mock
    private PspFeeRuleService pspFeeRuleService;

    private PayinPlanServiceImpl payinPlanService;

    @BeforeEach
    void setUp() {
        payinPlanService = new PayinPlanServiceImpl(
                merchantDao,
                merchantAppDao,
                merchantFeeRuleService,
                pspRouteSelector,
                pspFeeRuleService,
                new ObjectMapper(),
                new PayinPlanCache()
        );
    }

    @Test
    void resolveReturnsCachedPlanForSameBusinessKey() {
        PayOrderEntity order = payOrder();
        when(merchantFeeRuleService.calculatePayin(any(PayOrderEntity.class))).thenReturn(merchantFeeResult());
        when(pspRouteSelector.selectPayin(any(PayOrderEntity.class))).thenReturn(routeResult());
        when(pspFeeRuleService.calculatePayin(any(PayOrderEntity.class))).thenReturn(pspFeeResult());

        PayinPlan first = payinPlanService.resolve(order);
        PayinPlan second = payinPlanService.resolve(order);

        assertThat(second).isSameAs(first);
        assertThat(first.getMerchantFeeAmount()).isEqualByComparingTo("1.00");
        assertThat(first.getSettleAmount()).isEqualByComparingTo("99.00");
        assertThat(first.getPspFeeAmount()).isEqualByComparingTo("0.50");
        assertThat(first.getRoute().getPspCode()).isEqualTo("MAYA");
        verify(merchantFeeRuleService, times(1)).calculatePayin(any(PayOrderEntity.class));
        verify(pspRouteSelector, times(1)).selectPayin(any(PayOrderEntity.class));
        verify(pspFeeRuleService, times(1)).calculatePayin(any(PayOrderEntity.class));
    }

    @Test
    void precheckTreatsMissingPspFeeRuleAsWarning() {
        when(merchantDao.selectById(20L)).thenReturn(merchant());
        when(merchantAppDao.selectById(30L)).thenReturn(merchantApp());
        when(merchantFeeRuleService.calculatePayin(any(PayOrderEntity.class))).thenReturn(merchantFeeResult());
        when(pspRouteSelector.selectPayin(any(PayOrderEntity.class))).thenReturn(routeResult());
        when(pspFeeRuleService.calculatePayin(any(PayOrderEntity.class)))
                .thenThrow(new ApiException(ApiErrorCode.INVALID_REQUEST, "PSP fee rule is not configured"));

        PayinConfigPrecheckResult result = payinPlanService.precheck(precheckRequest());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).extracting(PayinConfigPrecheckResult.Item::getCode)
                .containsExactly("PSP_FEE_RULE_MISSING");
    }

    @Test
    void precheckTreatsNullPspFeeRuleAsWarning() {
        when(merchantDao.selectById(20L)).thenReturn(merchant());
        when(merchantAppDao.selectById(30L)).thenReturn(merchantApp());
        when(merchantFeeRuleService.calculatePayin(any(PayOrderEntity.class))).thenReturn(merchantFeeResult());
        when(pspRouteSelector.selectPayin(any(PayOrderEntity.class))).thenReturn(routeResult());
        when(pspFeeRuleService.calculatePayin(any(PayOrderEntity.class))).thenReturn(null);

        PayinConfigPrecheckResult result = payinPlanService.precheck(precheckRequest());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).extracting(PayinConfigPrecheckResult.Item::getCode)
                .containsExactly("PSP_FEE_RULE_MISSING");
    }

    private PayOrderEntity payOrder() {
        PayOrderEntity order = new PayOrderEntity();
        order.setTenantId(10L);
        order.setMerchantId(20L);
        order.setMerchantAppId(30L);
        order.setCountryCode("PH");
        order.setCurrency("PHP");
        order.setMethodCode("MAYA");
        order.setAmount(new BigDecimal("100.00"));
        return order;
    }

    private MerchantEntity merchant() {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(20L);
        merchant.setTenantId(10L);
        merchant.setMerchantNo("M20");
        merchant.setStatus(StatusEnum.NORMAL.code());
        return merchant;
    }

    private MerchantAppEntity merchantApp() {
        MerchantAppEntity app = new MerchantAppEntity();
        app.setId(30L);
        app.setTenantId(10L);
        app.setMerchantId(20L);
        app.setAppId("app_30");
        app.setStatus(StatusEnum.NORMAL.code());
        app.setAllowedCurrencyJson("[\"PHP\"]");
        app.setAllowedMethodJson("[\"MAYA\"]");
        return app;
    }

    private PayinConfigPrecheckRequest precheckRequest() {
        PayinConfigPrecheckRequest request = new PayinConfigPrecheckRequest();
        request.setTenantId(10L);
        request.setMerchantId(20L);
        request.setMerchantAppId(30L);
        request.setCountryCode("PH");
        request.setCurrency("PHP");
        request.setMethodCode("MAYA");
        request.setAmount(new BigDecimal("100.00"));
        return request;
    }

    private MerchantFeeResult merchantFeeResult() {
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setId(101L);
        MerchantFeeResult result = new MerchantFeeResult();
        result.setRule(rule);
        result.setMerchantFeeAmount(new BigDecimal("1.00"));
        result.setSettleAmount(new BigDecimal("99.00"));
        result.setSnapshotJson("{\"ruleId\":101}");
        return result;
    }

    private PspRouteResult routeResult() {
        PspRouteResult result = new PspRouteResult();
        result.setRouteRuleId(201L);
        result.setPspId(301L);
        result.setPspCode("MAYA");
        result.setPspMethodId(401L);
        result.setPspMethodCode("MAYA");
        result.setPspAccountId(501L);
        result.setPspAccountNo("PA501");
        return result;
    }

    private PspFeeResult pspFeeResult() {
        PspFeeRuleEntity rule = new PspFeeRuleEntity();
        rule.setId(601L);
        PspFeeResult result = new PspFeeResult();
        result.setRule(rule);
        result.setPspFeeAmount(new BigDecimal("0.50"));
        result.setSnapshotJson("{\"ruleId\":601}");
        return result;
    }
}
