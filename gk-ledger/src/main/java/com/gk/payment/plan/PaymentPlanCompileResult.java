package com.gk.payment.plan;

import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PaymentPlanCompileResult {
    private boolean valid = true;
    private PaymentPlanCatalogEntity catalog;
    private List<CompiledBucket> buckets = new ArrayList<>();
    private List<TestResult> testResults = new ArrayList<>();
    private List<Message> warnings = new ArrayList<>();
    private List<Message> errors = new ArrayList<>();

    public void addWarning(String code, String message) {
        warnings.add(new Message(code, message));
    }

    public void addError(String code, String message) {
        valid = false;
        errors.add(new Message(code, message));
    }

    public record Message(String code, String message) {
    }

    @Data
    public static class CompiledBucket {
        private PaymentPlanBucketEntity bucket;
        private MerchantFeeRuleEntity merchantFeeRule;
        private List<PaymentPlanRouteOptionEntity> routeOptions = new ArrayList<>();
        private List<CompiledRouteOption> routeOptionDetails = new ArrayList<>();
    }

    @Data
    public static class CompiledRouteOption {
        private PaymentPlanRouteOptionEntity option;
        private PspRouteRuleEntity routeRule;
        private PspProviderEntity provider;
        private PspMethodEntity method;
        private PspAccountEntity account;
        private PspFeeRuleEntity pspFeeRule;
    }

    @Data
    public static class TestResult {
        private String merchantOrderId;
        private BigDecimal amount;
        private boolean matched;
        private String message;
        private Long merchantFeeRuleId;
        private BigDecimal merchantFeeAmount;
        private BigDecimal settleAmount;
        private BigDecimal totalDebitAmount;
        private Long pspId;
        private String pspCode;
        private Long pspAccountId;
        private Long pspFeeRuleId;
        private BigDecimal pspFeeAmount;
        private String bankCode;
        private String pspBankCode;
    }
}
