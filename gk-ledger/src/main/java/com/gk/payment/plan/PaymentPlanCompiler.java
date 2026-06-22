package com.gk.payment.plan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.payment.dao.MerchantFeeRuleDao;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.payment.fee.MerchantFeeAmount;
import com.gk.payment.fee.MerchantFeeCalculator;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.fee.PspFeeCalculator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Compiles current payment configuration into payment plan catalog data.
 */
@Component
@RequiredArgsConstructor
public class PaymentPlanCompiler {
    private static final int DEFAULT_PRIORITY = 100;

    private final MerchantFeeRuleDao merchantFeeRuleDao;
    private final PspRouteRuleDao pspRouteRuleDao;
    private final PspFeeRuleDao pspFeeRuleDao;
    private final PspProviderDao pspProviderDao;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;
    private final PspBankMappingDao pspBankMappingDao;

    public PaymentPlanCompileResult compile(PaymentPlanCompileRequest request) {
        PaymentPlanCompileResult result = new PaymentPlanCompileResult();
        normalize(request);
        validate(request, result);
        if (!result.isValid()) {
            return result;
        }

        List<MerchantFeeRuleEntity> merchantRules = merchantRules(request);
        List<PspRouteRuleEntity> routeRules = routeRules(request);
        if (merchantRules.isEmpty()) {
            result.addError("MERCHANT_FEE_RULE_MISSING", "Merchant fee rule is not configured");
        }
        if (routeRules.isEmpty()) {
            result.addError("PSP_ROUTE_RULE_MISSING", "No available PSP route");
        }
        if (!result.isValid()) {
            return result;
        }

        List<PspFeeRuleEntity> pspFeeRules = pspFeeRules(request);
        PaymentPlanCatalogEntity catalog = catalog(request);
        List<PaymentPlanAmountRange> ranges = PaymentPlanAmountRangeSplitter.split(
                request.getMinAmount(),
                request.getMaxAmount(),
                sourceRanges(merchantRules, routeRules, pspFeeRules)
        );
        int bucketSort = 0;
        int routeOptionCount = 0;
        for (PaymentPlanAmountRange range : ranges) {
            PaymentPlanCompileResult.CompiledBucket compiledBucket = compileBucket(request, range, bucketSort++, routeRules, result);
            if (compiledBucket != null) {
                routeOptionCount += compiledBucket.getRouteOptions().size();
                result.getBuckets().add(compiledBucket);
            }
        }

        catalog.setBucketCount(result.getBuckets().size());
        catalog.setRouteOptionCount(routeOptionCount);
        catalog.setConfigHash(configHash(result));
        result.setCatalog(catalog);
        runTestCases(request, result);
        if (result.getBuckets().isEmpty()) {
            result.addError("PAYMENT_PLAN_EMPTY", "Payment plan bucket is empty");
        }
        return result;
    }

    private PaymentPlanCompileResult.CompiledBucket compileBucket(PaymentPlanCompileRequest request,
                                                                 PaymentPlanAmountRange range,
                                                                 int bucketSort,
                                                                 List<PspRouteRuleEntity> routeRules,
                                                                 PaymentPlanCompileResult result) {
        BigDecimal sampleAmount = range.startAmount();
        MerchantFeeRuleEntity merchantRule = merchantFeeRuleDao.selectBestMatchForOrder(
                request.getTenantId(),
                request.getMerchantId(),
                request.getMerchantAppId(),
                request.getCountryCode(),
                request.getCurrency(),
                request.getMethodCode(),
                sampleAmount,
                request.getDirection(),
                Instant.now(),
                StatusEnum.NORMAL.code()
        );
        if (merchantRule == null) {
            result.addError("MERCHANT_FEE_RULE_MISSING", "Merchant fee rule is not configured for amount " + sampleAmount);
            return null;
        }

        PaymentPlanBucketEntity bucket = new PaymentPlanBucketEntity();
        bucket.setTenantId(request.getTenantId());
        bucket.setBucketStartAmount(range.startAmount());
        bucket.setBucketEndAmount(range.endAmount());
        bucket.setMerchantFeeRuleId(merchantRule.getId());
        bucket.setMerchantFeeSnapshotJson(merchantSnapshotJson(merchantRule));
        bucket.setSort(bucketSort);

        List<PaymentPlanRouteOptionEntity> routeOptions = matchingRouteRules(routeRules, sampleAmount).stream()
                .map(rule -> routeOption(request, rule, sampleAmount, result))
                .filter(Objects::nonNull)
                .toList();
        if (routeOptions.isEmpty()) {
            result.addError("PSP_ROUTE_RULE_MISSING", "No available PSP route for amount " + sampleAmount);
            return null;
        }

        PaymentPlanCompileResult.CompiledBucket compiledBucket = new PaymentPlanCompileResult.CompiledBucket();
        compiledBucket.setBucket(bucket);
        compiledBucket.setRouteOptions(routeOptions);
        return compiledBucket;
    }

    private PaymentPlanRouteOptionEntity routeOption(PaymentPlanCompileRequest request,
                                                     PspRouteRuleEntity rule,
                                                     BigDecimal sampleAmount,
                                                     PaymentPlanCompileResult result) {
        PspProviderEntity provider = pspProviderDao.selectById(rule.getPspId());
        PspMethodEntity method = pspMethodDao.selectById(rule.getPspMethodId());
        PspAccountEntity account = pspAccountDao.selectById(rule.getPspAccountId());
        if (!resourceAvailable(request.getDirection(), provider, method, account)) {
            result.addWarning("PSP_RESOURCE_UNAVAILABLE", "PSP resource is unavailable for route rule " + rule.getId());
            return null;
        }

        if (requiresBankMapping(request) && !hasAnyBankMapping(request, rule.getPspId())) {
            result.addWarning("PSP_BANK_MAPPING_MISSING", "PSP bank mapping is not configured for PSP " + rule.getPspId());
            return null;
        }

        PspFeeRuleEntity feeRule = pspFeeRuleDao.selectBestMatchForOrder(
                request.getTenantId(),
                rule.getPspId(),
                rule.getPspAccountId(),
                rule.getPspMethodId(),
                request.getCountryCode(),
                request.getCurrency(),
                request.getMethodCode(),
                sampleAmount,
                request.getDirection(),
                Instant.now(),
                StatusEnum.NORMAL.code()
        );
        if (feeRule == null && Boolean.TRUE.equals(request.getPspFeeRequired())) {
            result.addWarning("PSP_FEE_RULE_MISSING", "PSP fee rule is not configured for route rule " + rule.getId());
            return null;
        }

        PaymentPlanRouteOptionEntity option = new PaymentPlanRouteOptionEntity();
        option.setTenantId(request.getTenantId());
        option.setRouteRuleId(rule.getId());
        option.setPspId(rule.getPspId());
        option.setPspCode(provider.getPspCode());
        option.setPspMethodId(rule.getPspMethodId());
        option.setPspMethodCode(method.getPspMethodCode());
        option.setPspAccountId(rule.getPspAccountId());
        option.setPspAccountNo(account.getPspAccountNo());
        option.setPspFeeRuleId(feeRule == null ? null : feeRule.getId());
        option.setPspFeeSnapshotJson(feeRule == null ? null : pspFeeSnapshotJson(feeRule));
        option.setPriority(defaultInt(rule.getPriority(), DEFAULT_PRIORITY));
        option.setWeight(defaultInt(rule.getWeight(), DEFAULT_PRIORITY));
        option.setFallbackOrder(defaultInt(rule.getPriority(), DEFAULT_PRIORITY));
        option.setStatus(PaymentPlanRouteOptionStatus.ACTIVE);
        option.setSort(defaultInt(rule.getPriority(), DEFAULT_PRIORITY));
        return option;
    }

    private void runTestCases(PaymentPlanCompileRequest request, PaymentPlanCompileResult result) {
        if (request.getTestCases() == null || request.getTestCases().isEmpty() || result.getBuckets().isEmpty()) {
            return;
        }
        List<PaymentPlanBucket> planBuckets = result.getBuckets().stream()
                .map(item -> {
                    PaymentPlanBucket bucket = new PaymentPlanBucket();
                    bucket.setBucket(item.getBucket());
                    bucket.setRouteOptions(item.getRouteOptions());
                    return bucket;
                })
                .toList();
        for (PaymentPlanCompileRequest.TestCase testCase : request.getTestCases()) {
            result.getTestResults().add(testCase(request, planBuckets, testCase));
        }
    }

    private PaymentPlanCompileResult.TestResult testCase(PaymentPlanCompileRequest request,
                                                        List<PaymentPlanBucket> planBuckets,
                                                        PaymentPlanCompileRequest.TestCase testCase) {
        PaymentPlanCompileResult.TestResult testResult = new PaymentPlanCompileResult.TestResult();
        testResult.setMerchantOrderId(testCase.getMerchantOrderId());
        testResult.setAmount(testCase.getAmount());
        testResult.setBankCode(normalize(testCase.getBankCode()));
        try {
            PaymentPlanBucket bucket = PaymentPlanBucketMatcher.matchPlanBucket(planBuckets, testCase.getAmount());
            PaymentPlanRouteOptionEntity option = PaymentPlanRouteOptionSelector.select(
                    bucket.getRouteOptions(),
                    StringUtils.defaultString(testCase.getMerchantOrderId()),
                    java.util.Set.of(),
                    java.util.Set.of(),
                    item -> testBankSupported(request, item, testCase.getBankCode())
            );
            MerchantFeeRuleEntity merchantRule = merchantRule(bucket.getBucket());
            MerchantFeeAmount merchantFee = MerchantFeeCalculator.calculate(testCase.getAmount(), merchantRule);
            PspFeeRuleEntity pspFeeRule = option.getPspFeeRuleId() == null ? null : pspFeeRule(option);
            BigDecimal pspFeeAmount = pspFeeRule == null ? BigDecimal.ZERO : PspFeeCalculator.calculate(testCase.getAmount(), pspFeeRule);
            testResult.setMatched(true);
            testResult.setMessage("OK");
            testResult.setMerchantFeeRuleId(bucket.getBucket().getMerchantFeeRuleId());
            testResult.setMerchantFeeAmount(merchantFee.feeAmount());
            testResult.setSettleAmount(merchantFee.payinSettleAmount());
            testResult.setTotalDebitAmount(testCase.getAmount().add(merchantFee.feeAmount()));
            testResult.setPspId(option.getPspId());
            testResult.setPspCode(option.getPspCode());
            testResult.setPspAccountId(option.getPspAccountId());
            testResult.setPspFeeRuleId(option.getPspFeeRuleId());
            testResult.setPspFeeAmount(pspFeeAmount);
            testResult.setPspBankCode(bankMapping(request, option, testCase.getBankCode()).map(PspBankMappingEntity::getPspBankCode).orElse(null));
        } catch (Exception ex) {
            testResult.setMatched(false);
            testResult.setMessage(ex.getMessage());
        }
        return testResult;
    }

    private MerchantFeeRuleEntity merchantRule(PaymentPlanBucketEntity bucket) {
        return merchantFeeRuleDao.selectById(bucket.getMerchantFeeRuleId());
    }

    private PspFeeRuleEntity pspFeeRule(PaymentPlanRouteOptionEntity option) {
        return pspFeeRuleDao.selectById(option.getPspFeeRuleId());
    }

    private boolean testBankSupported(PaymentPlanCompileRequest request, PaymentPlanRouteOptionEntity option, String bankCode) {
        if (!requiresBankMapping(request) || StringUtils.isBlank(bankCode)) {
            return !requiresBankMapping(request);
        }
        return bankMapping(request, option, bankCode).isPresent();
    }

    private java.util.Optional<PspBankMappingEntity> bankMapping(PaymentPlanCompileRequest request,
                                                                 PaymentPlanRouteOptionEntity option,
                                                                 String bankCode) {
        if (StringUtils.isBlank(bankCode)) {
            return java.util.Optional.empty();
        }
        PspBankMappingEntity mapping = pspBankMappingDao.selectOne(new QueryWrapper<PspBankMappingEntity>()
                .eq("psp_id", option.getPspId())
                .eq("country_code", request.getCountryCode())
                .eq("currency", request.getCurrency())
                .eq("bank_code", normalize(bankCode))
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
        return java.util.Optional.ofNullable(mapping).filter(item -> StringUtils.isNotBlank(item.getPspBankCode()));
    }

    private List<MerchantFeeRuleEntity> merchantRules(PaymentPlanCompileRequest request) {
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<MerchantFeeRuleEntity>()
                .eq("tenant_id", request.getTenantId())
                .eq("merchant_id", request.getMerchantId())
                .eq("direction", request.getDirection())
                .eq("currency", request.getCurrency())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("merchant_app_id", request.getMerchantAppId()).or().isNull("merchant_app_id"))
                .and(item -> item.eq("method_code", request.getMethodCode()).or().isNull("method_code").or().eq("method_code", ""))
                .and(item -> item.le("min_amount", request.getMaxAmount()).or().isNull("min_amount"))
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount"))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"));
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.and(item -> item.eq("country_code", request.getCountryCode()).or().isNull("country_code").or().eq("country_code", ""));
        }
        return merchantFeeRuleDao.selectList(wrapper);
    }

    private List<PspRouteRuleEntity> routeRules(PaymentPlanCompileRequest request) {
        QueryWrapper<PspRouteRuleEntity> wrapper = new QueryWrapper<PspRouteRuleEntity>()
                .eq("tenant_id", request.getTenantId())
                .eq("currency", request.getCurrency())
                .eq("method_code", request.getMethodCode())
                .eq("direction", request.getDirection())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("merchant_id", request.getMerchantId()).or().isNull("merchant_id"))
                .and(item -> item.eq("merchant_app_id", request.getMerchantAppId()).or().isNull("merchant_app_id"))
                .and(item -> item.le("min_amount", request.getMaxAmount()).or().isNull("min_amount"))
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount"))
                .orderByAsc("priority")
                .orderByAsc("id");
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.eq("country_code", request.getCountryCode());
        }
        return pspRouteRuleDao.selectList(wrapper).stream()
                .filter(this::timeAvailable)
                .sorted(routeOrder(request))
                .toList();
    }

    private List<PspRouteRuleEntity> matchingRouteRules(List<PspRouteRuleEntity> routeRules, BigDecimal amount) {
        return routeRules.stream()
                .filter(rule -> contains(rule.getMinAmount(), rule.getMaxAmount(), amount))
                .toList();
    }

    private List<PspFeeRuleEntity> pspFeeRules(PaymentPlanCompileRequest request) {
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<PspFeeRuleEntity>()
                .eq("tenant_id", request.getTenantId())
                .eq("direction", request.getDirection())
                .eq("currency", request.getCurrency())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("method_code", request.getMethodCode()).or().isNull("method_code").or().eq("method_code", ""))
                .and(item -> item.le("min_amount", request.getMaxAmount()).or().isNull("min_amount"))
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount"))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"));
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.and(item -> item.eq("country_code", request.getCountryCode()).or().isNull("country_code").or().eq("country_code", ""));
        } else {
            wrapper.and(item -> item.isNull("country_code").or().eq("country_code", ""));
        }
        return pspFeeRuleDao.selectList(wrapper);
    }

    private List<PaymentPlanAmountRange> sourceRanges(List<MerchantFeeRuleEntity> merchantRules,
                                                      List<PspRouteRuleEntity> routeRules,
                                                      List<PspFeeRuleEntity> pspFeeRules) {
        List<PaymentPlanAmountRange> ranges = new ArrayList<>();
        merchantRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        routeRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        pspFeeRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        return ranges;
    }

    private PaymentPlanCatalogEntity catalog(PaymentPlanCompileRequest request) {
        PaymentPlanCatalogEntity catalog = new PaymentPlanCatalogEntity();
        catalog.setTenantId(request.getTenantId());
        catalog.setMerchantId(request.getMerchantId());
        catalog.setMerchantAppId(request.getMerchantAppId());
        catalog.setDirection(request.getDirection());
        catalog.setCountryCode(request.getCountryCode());
        catalog.setCurrency(request.getCurrency());
        catalog.setMethodCode(request.getMethodCode());
        catalog.setStatus(PaymentPlanStatus.STAGING);
        catalog.setCompiledAt(Instant.now());
        catalog.setRemark(request.getRemark());
        return catalog;
    }

    private void validate(PaymentPlanCompileRequest request, PaymentPlanCompileResult result) {
        if (request == null) {
            result.addError("REQUEST_EMPTY", "request is required");
            return;
        }
        if (request.getTenantId() == null) {
            result.addError("TENANT_ID_REQUIRED", "tenantId is required");
        }
        if (request.getMerchantId() == null) {
            result.addError("MERCHANT_ID_REQUIRED", "merchantId is required");
        }
        if (request.getMerchantAppId() == null) {
            result.addError("MERCHANT_APP_ID_REQUIRED", "merchantAppId is required");
        }
        if (!PayDirectionEnum.PAYIN.code().equals(request.getDirection()) && !PayDirectionEnum.PAYOUT.code().equals(request.getDirection())) {
            result.addError("DIRECTION_INVALID", "direction must be PAYIN or PAYOUT");
        }
        if (StringUtils.isBlank(request.getCurrency())) {
            result.addError("CURRENCY_REQUIRED", "currency is required");
        }
        if (StringUtils.isBlank(request.getMethodCode())) {
            result.addError("METHOD_CODE_REQUIRED", "methodCode is required");
        }
        if (PayDirectionEnum.PAYOUT.code().equals(request.getDirection()) && StringUtils.isBlank(request.getCountryCode())) {
            result.addError("COUNTRY_CODE_REQUIRED", "countryCode is required for payout");
        }
        if (request.getMinAmount() == null || request.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("MIN_AMOUNT_INVALID", "minAmount must be greater than 0");
        }
        if (request.getMaxAmount() == null || request.getMaxAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("MAX_AMOUNT_INVALID", "maxAmount must be greater than 0");
        }
        if (request.getMinAmount() != null && request.getMaxAmount() != null
                && request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            result.addError("AMOUNT_RANGE_INVALID", "minAmount must be less than or equal to maxAmount");
        }
    }

    private void normalize(PaymentPlanCompileRequest request) {
        if (request == null) {
            return;
        }
        request.setDirection(normalize(request.getDirection()));
        request.setCurrency(normalize(request.getCurrency()));
        request.setMethodCode(normalize(request.getMethodCode()));
        String countryCode = PayDirectionEnum.PAYIN.code().equals(request.getDirection()) ? "" : normalize(request.getCountryCode());
        request.setCountryCode(countryCode);
        if (request.getPspFeeRequired() == null) {
            request.setPspFeeRequired(Boolean.TRUE);
        }
    }

    private boolean resourceAvailable(String direction,
                                      PspProviderEntity provider,
                                      PspMethodEntity method,
                                      PspAccountEntity account) {
        if (provider == null || method == null || account == null) {
            return false;
        }
        boolean providerSupported = PayDirectionEnum.PAYOUT.code().equals(direction)
                ? Integer.valueOf(1).equals(provider.getSupportPayout())
                : Integer.valueOf(1).equals(provider.getSupportPayin());
        return StatusEnum.NORMAL.code().equals(provider.getStatus())
                && providerSupported
                && StatusEnum.NORMAL.code().equals(method.getStatus())
                && StatusEnum.NORMAL.code().equals(account.getStatus());
    }

    private boolean hasAnyBankMapping(PaymentPlanCompileRequest request, Long pspId) {
        return pspBankMappingDao.selectCount(new QueryWrapper<PspBankMappingEntity>()
                .eq("psp_id", pspId)
                .eq("country_code", request.getCountryCode())
                .eq("currency", request.getCurrency())
                .eq("status", StatusEnum.NORMAL.code())) > 0;
    }

    private boolean requiresBankMapping(PaymentPlanCompileRequest request) {
        return PayDirectionEnum.PAYOUT.code().equals(request.getDirection())
                && (StringUtils.containsIgnoreCase(request.getMethodCode(), "BANK")
                || StringUtils.containsIgnoreCase(request.getMethodCode(), "CARD"));
    }

    private boolean timeAvailable(PspRouteRuleEntity rule) {
        LocalTime start = rule.getStartTime();
        LocalTime end = rule.getEndTime();
        LocalTime now = LocalTime.now();
        return start == null
                || end == null
                || start.equals(end)
                || (start.isBefore(end) && !now.isBefore(start) && !now.isAfter(end))
                || (start.isAfter(end) && (!now.isBefore(start) || !now.isAfter(end)));
    }

    private boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        return (minAmount == null || minAmount.compareTo(amount) <= 0)
                && (maxAmount == null || maxAmount.compareTo(amount) >= 0);
    }

    private Comparator<PspRouteRuleEntity> routeOrder(PaymentPlanCompileRequest request) {
        return Comparator
                .comparingInt((PspRouteRuleEntity rule) -> routeSpecificity(rule, request))
                .thenComparingInt(rule -> defaultInt(rule.getPriority(), DEFAULT_PRIORITY))
                .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId());
    }

    private int routeSpecificity(PspRouteRuleEntity rule, PaymentPlanCompileRequest request) {
        if (Objects.equals(rule.getMerchantId(), request.getMerchantId())
                && Objects.equals(rule.getMerchantAppId(), request.getMerchantAppId())) {
            return 0;
        }
        if (Objects.equals(rule.getMerchantId(), request.getMerchantId()) && rule.getMerchantAppId() == null) {
            return 10;
        }
        if (rule.getMerchantId() == null && rule.getMerchantAppId() == null) {
            return 20;
        }
        return DEFAULT_PRIORITY;
    }

    private String merchantSnapshotJson(MerchantFeeRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("direction", rule.getDirection());
        snapshot.put("countryCode", rule.getCountryCode());
        snapshot.put("currency", rule.getCurrency());
        snapshot.put("methodCode", rule.getMethodCode());
        snapshot.put("feeMode", rule.getFeeMode());
        snapshot.put("feeRate", decimalText(rule.getFeeRate()));
        snapshot.put("feeFixed", decimalText(rule.getFeeFixed()));
        snapshot.put("minFee", decimalText(rule.getMinFee()));
        snapshot.put("maxFee", decimalText(rule.getMaxFee()));
        snapshot.put("feeBearer", rule.getFeeBearer());
        snapshot.put("settleMode", rule.getSettleMode());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String pspFeeSnapshotJson(PspFeeRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("pspId", rule.getPspId());
        snapshot.put("pspAccountId", rule.getPspAccountId());
        snapshot.put("pspMethodId", rule.getPspMethodId());
        snapshot.put("pspMethodCode", rule.getPspMethodCode());
        snapshot.put("direction", rule.getDirection());
        snapshot.put("countryCode", rule.getCountryCode());
        snapshot.put("currency", rule.getCurrency());
        snapshot.put("methodCode", rule.getMethodCode());
        snapshot.put("feeMode", rule.getFeeMode());
        snapshot.put("feeRate", decimalText(rule.getFeeRate()));
        snapshot.put("feeFixed", decimalText(rule.getFeeFixed()));
        snapshot.put("minFee", decimalText(rule.getMinFee()));
        snapshot.put("maxFee", decimalText(rule.getMaxFee()));
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String configHash(PaymentPlanCompileResult result) {
        List<Object> source = new ArrayList<>();
        for (PaymentPlanCompileResult.CompiledBucket bucket : result.getBuckets()) {
            source.add(bucket.getBucket().getBucketStartAmount());
            source.add(bucket.getBucket().getBucketEndAmount());
            source.add(bucket.getBucket().getMerchantFeeRuleId());
            bucket.getRouteOptions().forEach(option -> {
                source.add(option.getRouteRuleId());
                source.add(option.getPspFeeRuleId());
                source.add(option.getPriority());
                source.add(option.getWeight());
            });
        }
        return Integer.toHexString(JSON.toJSONString(source).hashCode());
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
