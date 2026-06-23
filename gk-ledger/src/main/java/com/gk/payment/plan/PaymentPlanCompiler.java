package com.gk.payment.plan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.payment.constant.PaymentMethodCodes;
import com.gk.payment.dao.MerchantFeeRuleDao;
import com.gk.payment.dao.PaymentRouteChannelDao;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dao.PaymentRouteRuleDao;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.payment.entity.PaymentRouteChannelEntity;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.entity.PaymentRouteRuleEntity;
import com.gk.payment.fee.MerchantFeeAmount;
import com.gk.payment.fee.MerchantFeeCalculator;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.fee.PspFeeCalculator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final PaymentRouteRuleDao paymentRouteRuleDao;
    private final PaymentRouteGroupDao paymentRouteGroupDao;
    private final PaymentRouteChannelDao paymentRouteChannelDao;
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
        List<PaymentRouteRuleEntity> paymentRouteRules = paymentRouteRules(request);
        List<PaymentRouteChannelEntity> paymentRouteChannels = paymentRouteChannels(paymentRouteRules);
        if (merchantRules.isEmpty()) {
            result.addError("MERCHANT_FEE_RULE_MISSING", "Merchant fee rule is not configured");
        }
        if (paymentRouteRules.isEmpty()) {
            result.addError("PSP_ROUTE_RULE_MISSING", "Payment route rule is not configured");
        }
        if (!result.isValid()) {
            return result;
        }

        List<PspFeeRuleEntity> pspFeeRules = pspFeeRules(request);
        PaymentPlanCatalogEntity catalog = catalog(request);
        List<PaymentPlanAmountRange> ranges = PaymentPlanAmountRangeSplitter.split(
                request.getMinAmount(),
                request.getMaxAmount(),
                sourceRanges(merchantRules, paymentRouteRules, paymentRouteChannels, pspFeeRules)
        );
        int bucketSort = 0;
        int routeOptionCount = 0;
        for (PaymentPlanAmountRange range : ranges) {
            PaymentPlanCompileResult.CompiledBucket compiledBucket = compileBucket(request, range, bucketSort++, merchantRules, paymentRouteRules, result);
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
                                                                 List<MerchantFeeRuleEntity> merchantRules,
                                                                 List<PaymentRouteRuleEntity> paymentRouteRules,
                                                                 PaymentPlanCompileResult result) {
        BigDecimal sampleAmount = range.startAmount();
        MerchantFeeRuleEntity merchantRule = merchantRule(request, merchantRules, sampleAmount);
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

        List<PaymentPlanCompileResult.CompiledRouteOption> routeOptionDetails = paymentRouteOptions(request, paymentRouteRules, sampleAmount, result);
        List<PaymentPlanRouteOptionEntity> routeOptions = routeOptionDetails.stream()
                .map(PaymentPlanCompileResult.CompiledRouteOption::getOption)
                .toList();
        if (routeOptions.isEmpty()) {
            result.addError("PSP_ROUTE_RULE_MISSING", "No available PSP route for amount " + sampleAmount);
            return null;
        }

        PaymentPlanCompileResult.CompiledBucket compiledBucket = new PaymentPlanCompileResult.CompiledBucket();
        compiledBucket.setBucket(bucket);
        compiledBucket.setMerchantFeeRule(merchantRule);
        compiledBucket.setRouteOptions(routeOptions);
        compiledBucket.setRouteOptionDetails(routeOptionDetails);
        return compiledBucket;
    }

    private MerchantFeeRuleEntity merchantRule(PaymentPlanCompileRequest request,
                                               List<MerchantFeeRuleEntity> merchantRules,
                                               BigDecimal sampleAmount) {
        if (request.getMerchantFeeRuleId() != null) {
            return merchantRules.stream()
                    .filter(rule -> Objects.equals(rule.getId(), request.getMerchantFeeRuleId()))
                    .filter(rule -> contains(rule.getMinAmount(), rule.getMaxAmount(), sampleAmount))
                    .findFirst()
                    .orElse(null);
        }
        return merchantFeeRuleDao.selectBestMatchForOrder(
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
    }

    private List<PaymentPlanCompileResult.CompiledRouteOption> paymentRouteOptions(PaymentPlanCompileRequest request,
                                                                                  List<PaymentRouteRuleEntity> routeRules,
                                                                                  BigDecimal sampleAmount,
                                                                                  PaymentPlanCompileResult result) {
        PaymentRouteRuleEntity routeRule = matchingPaymentRouteRules(routeRules, sampleAmount).stream()
                .findFirst()
                .orElse(null);
        if (routeRule == null) {
            result.addError("PSP_ROUTE_RULE_MISSING", "No available payment route rule for amount " + sampleAmount);
            return List.of();
        }

        PaymentRouteGroupEntity group = paymentRouteGroupDao.selectById(routeRule.getGroupId());
        if (!routeGroupMatches(routeRule, group)) {
            result.addWarning("PAYMENT_ROUTE_GROUP_INVALID", "Payment route group does not match route rule " + routeRule.getId());
            return List.of();
        }

        List<PaymentRouteChannelEntity> channels = paymentRouteChannelDao.selectList(new QueryWrapper<PaymentRouteChannelEntity>()
                        .eq("tenant_id", routeRule.getTenantId())
                        .eq("group_id", routeRule.getGroupId())
                        .eq("status", StatusEnum.NORMAL.code())
                        .and(item -> item.le("min_amount", sampleAmount).or().isNull("min_amount"))
                        .and(item -> item.ge("max_amount", sampleAmount).or().isNull("max_amount"))
                        .orderByAsc("priority")
                        .orderByAsc("fallback_order")
                        .orderByAsc("id"))
                .stream()
                .filter(channel -> contains(channel.getMinAmount(), channel.getMaxAmount(), sampleAmount))
                .toList();
        if (channels.isEmpty()) {
            result.addError("PAYMENT_ROUTE_CHANNEL_MISSING", "No available payment route channel for route rule " + routeRule.getId());
            return List.of();
        }

        return channels.stream()
                .map(channel -> routeOption(request, routeRule, group, channel, sampleAmount, result))
                .filter(Objects::nonNull)
                .toList();
    }

    private PaymentPlanCompileResult.CompiledRouteOption routeOption(PaymentPlanCompileRequest request,
                                                                     PaymentRouteRuleEntity routeRule,
                                                                     PaymentRouteGroupEntity group,
                                                                     PaymentRouteChannelEntity channel,
                                                                     BigDecimal sampleAmount,
                                                                     PaymentPlanCompileResult result) {
        PspProviderEntity provider = pspProviderDao.selectById(channel.getPspId());
        PspMethodEntity method = pspMethodDao.selectById(channel.getPspMethodId());
        PspAccountEntity account = pspAccountDao.selectById(channel.getPspAccountId());
        if (!resourceAvailable(request.getDirection(), provider, method, account)) {
            result.addWarning("PSP_RESOURCE_UNAVAILABLE", "PSP resource is unavailable for payment route channel " + channel.getId());
            return null;
        }
        if (!routeMethodMatches(request, method)) {
            result.addWarning("PSP_METHOD_NOT_MATCH", "PSP method does not match request method for payment route channel " + channel.getId());
            return null;
        }
        if (!routeChannelMatches(group, channel, method, account)) {
            result.addWarning("PAYMENT_ROUTE_CHANNEL_INVALID", "Payment route channel does not match route group " + group.getId());
            return null;
        }

        if (requiresBankMapping(request) && !hasAnyBankMapping(request, channel.getPspId())) {
            result.addWarning("PSP_BANK_MAPPING_MISSING", "PSP bank mapping is not configured for PSP " + channel.getPspId());
            return null;
        }

        PspFeeRuleEntity feeRule = pspFeeRule(request, channel, sampleAmount, result);
        if (feeRule == null) {
            result.addWarning("PSP_FEE_RULE_MISSING", "PSP fee rule is not configured for payment route channel " + channel.getId());
            if (Boolean.TRUE.equals(request.getPspFeeRequired())) {
                return null;
            }
        }

        PaymentPlanRouteOptionEntity option = new PaymentPlanRouteOptionEntity();
        option.setTenantId(request.getTenantId());
        option.setRouteRuleId(routeRule.getId());
        option.setRouteGroupId(group.getId());
        option.setRouteChannelId(channel.getId());
        option.setPspId(channel.getPspId());
        option.setPspCode(provider.getPspCode());
        option.setPspMethodId(channel.getPspMethodId());
        option.setPspMethodCode(method.getPspMethodCode());
        option.setPspAccountId(channel.getPspAccountId());
        option.setPspAccountNo(account.getPspAccountNo());
        option.setPspFeeRuleId(feeRule == null ? null : feeRule.getId());
        option.setPspFeeSnapshotJson(feeRule == null ? null : pspFeeSnapshotJson(feeRule));
        option.setRouteRuleSnapshotJson(paymentRouteRuleSnapshotJson(routeRule));
        option.setRouteGroupSnapshotJson(routeGroupSnapshotJson(group));
        option.setRouteChannelSnapshotJson(routeChannelSnapshotJson(channel));
        option.setPspProviderSnapshotJson(pspProviderSnapshotJson(provider));
        option.setPspMethodSnapshotJson(pspMethodSnapshotJson(method));
        option.setPspAccountSnapshotJson(pspAccountSnapshotJson(account));
        option.setPriority(defaultInt(channel.getPriority(), DEFAULT_PRIORITY));
        option.setWeight(defaultInt(channel.getWeight(), DEFAULT_PRIORITY));
        option.setFallbackOrder(defaultInt(channel.getFallbackOrder(), DEFAULT_PRIORITY));
        option.setStatus(PaymentPlanRouteOptionStatus.ACTIVE);
        option.setSort(defaultInt(channel.getPriority(), DEFAULT_PRIORITY));

        PaymentPlanCompileResult.CompiledRouteOption detail = new PaymentPlanCompileResult.CompiledRouteOption();
        detail.setOption(option);
        detail.setPaymentRouteRule(routeRule);
        detail.setRouteGroup(group);
        detail.setRouteChannel(channel);
        detail.setProvider(provider);
        detail.setMethod(method);
        detail.setAccount(account);
        detail.setPspFeeRule(feeRule);
        return detail;
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

    private PspFeeRuleEntity pspFeeRule(PaymentPlanCompileRequest request,
                                        PaymentRouteChannelEntity channel,
                                        BigDecimal sampleAmount,
                                        PaymentPlanCompileResult result) {
        if (channel.getPspFeeRuleId() != null) {
            PspFeeRuleEntity feeRule = pspFeeRuleDao.selectById(channel.getPspFeeRuleId());
            if (!pspFeeRuleMatches(request, channel, sampleAmount, feeRule)) {
                result.addWarning("PSP_FEE_RULE_INVALID", "PSP fee rule does not match payment route channel " + channel.getId());
                return null;
            }
            return feeRule;
        }
        return pspFeeRuleDao.selectBestMatchForOrder(
                request.getTenantId(),
                channel.getPspId(),
                channel.getPspAccountId(),
                channel.getPspMethodId(),
                request.getCountryCode(),
                request.getCurrency(),
                request.getMethodCode(),
                sampleAmount,
                request.getDirection(),
                Instant.now(),
                StatusEnum.NORMAL.code()
        );
    }

    private boolean pspFeeRuleMatches(PaymentPlanCompileRequest request,
                                      PaymentRouteChannelEntity channel,
                                      BigDecimal sampleAmount,
                                      PspFeeRuleEntity feeRule) {
        if (feeRule == null) {
            return false;
        }
        if (!StatusEnum.NORMAL.code().equals(feeRule.getStatus())) {
            return false;
        }
        if (!Objects.equals(request.getTenantId(), feeRule.getTenantId())) {
            return false;
        }
        if (!Objects.equals(channel.getPspId(), feeRule.getPspId())) {
            return false;
        }
        if (feeRule.getPspAccountId() != null && !Objects.equals(channel.getPspAccountId(), feeRule.getPspAccountId())) {
            return false;
        }
        if (feeRule.getPspMethodId() != null && !Objects.equals(channel.getPspMethodId(), feeRule.getPspMethodId())) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(request.getDirection()), StringUtils.trim(feeRule.getDirection()))) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(request.getCurrency()), StringUtils.trim(feeRule.getCurrency()))) {
            return false;
        }
        if (StringUtils.isNotBlank(feeRule.getMethodCode())
                && !StringUtils.equalsIgnoreCase(StringUtils.trim(request.getMethodCode()), StringUtils.trim(feeRule.getMethodCode()))) {
            return false;
        }
        if (StringUtils.isBlank(request.getCountryCode()) && StringUtils.isNotBlank(feeRule.getCountryCode())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getCountryCode())
                && StringUtils.isNotBlank(feeRule.getCountryCode())
                && !StringUtils.equalsIgnoreCase(StringUtils.trim(request.getCountryCode()), StringUtils.trim(feeRule.getCountryCode()))) {
            return false;
        }
        return contains(feeRule.getMinAmount(), feeRule.getMaxAmount(), sampleAmount);
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
        } else {
            wrapper.and(item -> item.isNull("country_code").or().eq("country_code", ""));
        }
        if (request.getMerchantFeeRuleId() != null) {
            wrapper.eq("id", request.getMerchantFeeRuleId());
        }
        return merchantFeeRuleDao.selectList(wrapper);
    }

    private List<PaymentRouteRuleEntity> paymentRouteRules(PaymentPlanCompileRequest request) {
        QueryWrapper<PaymentRouteRuleEntity> wrapper = new QueryWrapper<PaymentRouteRuleEntity>()
                .eq("tenant_id", request.getTenantId())
                .eq("currency", request.getCurrency())
                .eq("direction", request.getDirection())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("method_code", request.getMethodCode()).or().isNull("method_code").or().eq("method_code", ""))
                .and(item -> item.eq("merchant_id", request.getMerchantId()).or().isNull("merchant_id"))
                .and(item -> item.eq("merchant_app_id", request.getMerchantAppId()).or().isNull("merchant_app_id"))
                .and(item -> item.le("min_amount", request.getMaxAmount()).or().isNull("min_amount"))
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount"))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"))
                .orderByAsc("priority")
                .orderByAsc("id");
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.and(item -> item.eq("country_code", request.getCountryCode()).or().isNull("country_code").or().eq("country_code", ""));
        } else {
            wrapper.and(item -> item.isNull("country_code").or().eq("country_code", ""));
        }
        return paymentRouteRuleDao.selectList(wrapper).stream()
                .sorted(paymentRouteOrder(request))
                .toList();
    }

    private List<PaymentRouteChannelEntity> paymentRouteChannels(List<PaymentRouteRuleEntity> routeRules) {
        List<Long> groupIds = routeRules.stream()
                .map(PaymentRouteRuleEntity::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        Long tenantId = routeRules.get(0).getTenantId();
        return paymentRouteChannelDao.selectList(new QueryWrapper<PaymentRouteChannelEntity>()
                .eq("tenant_id", tenantId)
                .in("group_id", groupIds)
                .eq("status", StatusEnum.NORMAL.code()));
    }

    private List<PaymentRouteRuleEntity> matchingPaymentRouteRules(List<PaymentRouteRuleEntity> routeRules, BigDecimal amount) {
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
                                                      List<PaymentRouteRuleEntity> paymentRouteRules,
                                                      List<PaymentRouteChannelEntity> paymentRouteChannels,
                                                      List<PspFeeRuleEntity> pspFeeRules) {
        List<PaymentPlanAmountRange> ranges = new ArrayList<>();
        merchantRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        paymentRouteRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        paymentRouteChannels.forEach(channel -> ranges.add(PaymentPlanAmountRange.closed(channel.getMinAmount(), channel.getMaxAmount())));
        pspFeeRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        return ranges;
    }

    private PaymentPlanCatalogEntity catalog(PaymentPlanCompileRequest request) {
        PaymentPlanCatalogEntity catalog = new PaymentPlanCatalogEntity();
        catalog.setTenantId(request.getTenantId());
        catalog.setMerchantId(request.getMerchantId());
        catalog.setMerchantAppId(catalogMerchantAppId(request.getMerchantAppId()));
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
        if (!PayDirectionEnum.PAYIN.code().equals(request.getDirection()) && !PayDirectionEnum.PAYOUT.code().equals(request.getDirection())) {
            result.addError("DIRECTION_INVALID", "direction must be PAYIN or PAYOUT");
        }
        if (StringUtils.isBlank(request.getCurrency())) {
            result.addError("CURRENCY_REQUIRED", "currency is required");
        }
        if (StringUtils.isBlank(request.getMethodCode())) {
            result.addError("METHOD_CODE_REQUIRED", "methodCode is required");
        }
        if (request.getMinAmount() == null || request.getMinAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("MIN_AMOUNT_INVALID", "minAmount must be greater than or equal to 0");
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
        request.setCountryCode(normalize(request.getCountryCode()));
        if (request.getMerchantAppId() != null && request.getMerchantAppId() <= 0) {
            request.setMerchantAppId(null);
        }
        if (request.getMinAmount() == null) {
            request.setMinAmount(BigDecimal.ZERO);
        }
        if (request.getPspFeeRequired() == null) {
            request.setPspFeeRequired(Boolean.FALSE);
        }
    }

    private Long catalogMerchantAppId(Long merchantAppId) {
        return merchantAppId == null || merchantAppId <= 0 ? 0L : merchantAppId;
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
                && PaymentMethodCodes.isBankCard(request.getMethodCode());
    }

    private boolean contains(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal amount) {
        return (minAmount == null || minAmount.compareTo(amount) <= 0)
                && (maxAmount == null || maxAmount.compareTo(amount) >= 0);
    }

    private Comparator<PaymentRouteRuleEntity> paymentRouteOrder(PaymentPlanCompileRequest request) {
        return Comparator
                .comparingInt((PaymentRouteRuleEntity rule) -> paymentRouteSpecificity(rule, request))
                .thenComparingInt(rule -> defaultInt(rule.getPriority(), DEFAULT_PRIORITY))
                .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId());
    }

    private int paymentRouteSpecificity(PaymentRouteRuleEntity rule, PaymentPlanCompileRequest request) {
        int methodOffset = StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getMethodCode()), request.getMethodCode()) ? 0 : 1;
        int countryOffset = StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getCountryCode()), request.getCountryCode()) ? 0 : 2;
        if (Objects.equals(rule.getMerchantId(), request.getMerchantId())
                && Objects.equals(rule.getMerchantAppId(), request.getMerchantAppId())) {
            return countryOffset + methodOffset;
        }
        if (Objects.equals(rule.getMerchantId(), request.getMerchantId()) && rule.getMerchantAppId() == null) {
            return 10 + countryOffset + methodOffset;
        }
        if (rule.getMerchantId() == null && rule.getMerchantAppId() == null) {
            return 20 + countryOffset + methodOffset;
        }
        return DEFAULT_PRIORITY;
    }

    private boolean routeMethodMatches(PaymentPlanCompileRequest request, PspMethodEntity method) {
        if (method == null) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(request.getMethodCode()), StringUtils.trim(method.getMethodCode()))) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(request.getDirection()), StringUtils.trim(method.getDirection()))) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(request.getCurrency()), StringUtils.trim(method.getCurrency()))) {
            return false;
        }
        return StringUtils.isBlank(request.getCountryCode())
                || StringUtils.equalsIgnoreCase(StringUtils.trim(request.getCountryCode()), StringUtils.trim(method.getCountryCode()));
    }

    private boolean routeGroupMatches(PaymentRouteRuleEntity rule, PaymentRouteGroupEntity group) {
        if (rule == null || group == null) {
            return false;
        }
        return Objects.equals(rule.getTenantId(), group.getTenantId())
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getDirection()), StringUtils.trim(group.getDirection()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getCountryCode()), StringUtils.trim(group.getCountryCode()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getCurrency()), StringUtils.trim(group.getCurrency()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getMethodCode()), StringUtils.trim(group.getMethodCode()));
    }

    private boolean routeChannelMatches(PaymentRouteGroupEntity group,
                                        PaymentRouteChannelEntity channel,
                                        PspMethodEntity method,
                                        PspAccountEntity account) {
        if (group == null || channel == null || method == null || account == null) {
            return false;
        }
        return Objects.equals(group.getTenantId(), channel.getTenantId())
                && Objects.equals(channel.getPspId(), method.getPspId())
                && Objects.equals(channel.getPspId(), account.getPspId())
                && Objects.equals(group.getTenantId(), account.getTenantId())
                && StringUtils.equalsIgnoreCase(StringUtils.trim(group.getDirection()), StringUtils.trim(method.getDirection()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(group.getCurrency()), StringUtils.trim(method.getCurrency()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(group.getMethodCode()), StringUtils.trim(method.getMethodCode()))
                && (StringUtils.isBlank(group.getCountryCode())
                || StringUtils.equalsIgnoreCase(StringUtils.trim(group.getCountryCode()), StringUtils.trim(method.getCountryCode())));
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

    private String paymentRouteRuleSnapshotJson(PaymentRouteRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("merchantId", rule.getMerchantId());
        snapshot.put("merchantAppId", rule.getMerchantAppId());
        snapshot.put("direction", rule.getDirection());
        snapshot.put("countryCode", rule.getCountryCode());
        snapshot.put("currency", rule.getCurrency());
        snapshot.put("methodCode", rule.getMethodCode());
        snapshot.put("minAmount", decimalText(rule.getMinAmount()));
        snapshot.put("maxAmount", decimalText(rule.getMaxAmount()));
        snapshot.put("groupId", rule.getGroupId());
        snapshot.put("priority", rule.getPriority());
        snapshot.put("effectiveAt", rule.getEffectiveAt());
        snapshot.put("expireAt", rule.getExpireAt());
        snapshot.put("status", rule.getStatus());
        snapshot.put("remark", rule.getRemark());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String routeGroupSnapshotJson(PaymentRouteGroupEntity group) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("groupId", group.getId());
        snapshot.put("groupCode", group.getGroupCode());
        snapshot.put("groupName", group.getGroupName());
        snapshot.put("direction", group.getDirection());
        snapshot.put("countryCode", group.getCountryCode());
        snapshot.put("currency", group.getCurrency());
        snapshot.put("methodCode", group.getMethodCode());
        snapshot.put("strategy", group.getStrategy());
        snapshot.put("status", group.getStatus());
        snapshot.put("remark", group.getRemark());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String routeChannelSnapshotJson(PaymentRouteChannelEntity channel) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("routeChannelId", channel.getId());
        snapshot.put("groupId", channel.getGroupId());
        snapshot.put("pspId", channel.getPspId());
        snapshot.put("pspMethodId", channel.getPspMethodId());
        snapshot.put("pspAccountId", channel.getPspAccountId());
        snapshot.put("pspFeeRuleId", channel.getPspFeeRuleId());
        snapshot.put("priority", channel.getPriority());
        snapshot.put("weight", channel.getWeight());
        snapshot.put("fallbackOrder", channel.getFallbackOrder());
        snapshot.put("minAmount", decimalText(channel.getMinAmount()));
        snapshot.put("maxAmount", decimalText(channel.getMaxAmount()));
        snapshot.put("status", channel.getStatus());
        snapshot.put("remark", channel.getRemark());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String pspProviderSnapshotJson(PspProviderEntity provider) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pspId", provider.getId());
        snapshot.put("pspCode", provider.getPspCode());
        snapshot.put("pspName", provider.getPspName());
        snapshot.put("countryCode", provider.getCountryCode());
        snapshot.put("baseUrl", provider.getBaseUrl());
        snapshot.put("apiVersion", provider.getApiVersion());
        snapshot.put("supportPayin", provider.getSupportPayin());
        snapshot.put("supportPayout", provider.getSupportPayout());
        snapshot.put("configJson", provider.getConfigJson());
        snapshot.put("status", provider.getStatus());
        snapshot.put("remark", provider.getRemark());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String pspMethodSnapshotJson(PspMethodEntity method) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pspMethodId", method.getId());
        snapshot.put("pspId", method.getPspId());
        snapshot.put("pspCode", method.getPspCode());
        snapshot.put("methodCode", method.getMethodCode());
        snapshot.put("pspMethodCode", method.getPspMethodCode());
        snapshot.put("methodName", method.getMethodName());
        snapshot.put("countryCode", method.getCountryCode());
        snapshot.put("currency", method.getCurrency());
        snapshot.put("direction", method.getDirection());
        snapshot.put("minAmount", decimalText(method.getMinAmount()));
        snapshot.put("maxAmount", decimalText(method.getMaxAmount()));
        snapshot.put("dailyLimit", decimalText(method.getDailyLimit()));
        snapshot.put("configJson", method.getConfigJson());
        snapshot.put("status", method.getStatus());
        snapshot.put("remark", method.getRemark());
        return JSON.toJSONString(snapshot, JSONWriter.Feature.WriteMapNullValue);
    }

    private String pspAccountSnapshotJson(PspAccountEntity account) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("pspAccountId", account.getId());
        snapshot.put("tenantId", account.getTenantId());
        snapshot.put("pspId", account.getPspId());
        snapshot.put("pspAccountNo", account.getPspAccountNo());
        snapshot.put("pspAccountName", account.getPspAccountName());
        snapshot.put("secretType", account.getSecretType());
        snapshot.put("configJson", account.getConfigJson());
        snapshot.put("status", account.getStatus());
        snapshot.put("remark", account.getRemark());
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
                source.add(option.getRouteGroupId());
                source.add(option.getRouteChannelId());
                source.add(option.getPspFeeRuleId());
                source.add(option.getRouteRuleSnapshotJson());
                source.add(option.getRouteGroupSnapshotJson());
                source.add(option.getRouteChannelSnapshotJson());
                source.add(option.getPspProviderSnapshotJson());
                source.add(option.getPspMethodSnapshotJson());
                source.add(option.getPspAccountSnapshotJson());
                source.add(option.getPspFeeSnapshotJson());
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
