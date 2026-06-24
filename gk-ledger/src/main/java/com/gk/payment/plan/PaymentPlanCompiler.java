package com.gk.payment.plan;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.payment.amount.AmountRangeUtils;
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
 * 将可编辑的支付配置编译成 payment_plan_* 运行时方案数据。
 *
 * <p>预览和发布阶段读取 merchant_fee_rule、payment_route_rule、
 * payment_route_group、payment_route_channel 以及 PSP 资源表。订单运行时
 * 应该读取这里生成的 ACTIVE payment_plan_* 快照，而不是每笔订单实时关联
 * 源配置表。</p>
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

    /**
     * 预览/发布的编译入口。
     *
     * <p>这里会先加载完整的源配置，因此即使费率或路由配置缺失，预览接口
     * 也能返回尽量完整的诊断信息。</p>
     */
    public PaymentPlanCompileResult compile(PaymentPlanCompileRequest request) {
        PaymentPlanCompileResult result = new PaymentPlanCompileResult();
        normalize(request);
        result.setRequest(request);
        validate(request, result);
        if (!result.isValid()) {
            return result;
        }

        // 加载本次编译使用到的完整源配置，并放入结果中给预览页面展示。
        // 匹配商户费用规则
        List<MerchantFeeRuleEntity> merchantRules = merchantRules(request);
        // 匹配商户路由规则
        List<PaymentRouteRuleEntity> paymentRouteRules = paymentRouteRules(request);
        // 根据路由组获取对应的PSP渠道
        List<PaymentRouteChannelEntity> paymentRouteChannels = paymentRouteChannels(paymentRouteRules);
        // 获取商户路由对应的路由组
        List<PaymentRouteGroupEntity> paymentRouteGroups = paymentRouteGroups(paymentRouteRules);
        // 获取PSP 对应的编码
        List<PspMethodEntity> pspMethods = pspMethods(paymentRouteChannels);
        // 获取命中通道对应的 PSP 成本规则，优先使用通道固定的 psp_fee_rule_id。
        List<PspFeeRuleEntity> pspFeeRules = pspFeeRules(request, paymentRouteGroups, paymentRouteChannels, pspMethods);

        PaymentPlanCatalogEntity catalog = catalog(request);
        result.setCatalog(catalog);
        result.setMerchantFeeRules(merchantRules);
        result.setPaymentRouteRules(paymentRouteRules);
        result.setRouteGroups(paymentRouteGroups);
        result.setRouteChannels(paymentRouteChannels);
        result.setPspMethods(pspMethods);
        result.setPspFeeRules(pspFeeRules);
        if (merchantRules.isEmpty()) {
            result.addError("MERCHANT_FEE_RULE_MISSING", "Merchant fee rule is not configured");
        }
        if (paymentRouteRules.isEmpty()) {
            result.addError("PAYMENT_ROUTE_RULE_MISSING", "Payment route rule is not configured");
        }
        if (merchantRules.isEmpty() && paymentRouteRules.isEmpty()) {
            return result;
        }

        // 按所有源配置的金额边界切分区间，保证每个 bucket 内费率和路由结果稳定。
        List<PaymentPlanAmountRange> ranges = PaymentPlanAmountRangeSplitter.split(
                request.getMinAmount(),
                request.getMaxAmount(),
                sourceRanges(merchantRules, paymentRouteRules, paymentRouteChannels, pspMethods, pspFeeRules)
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
        runTestCases(request, result);
        if (result.getBuckets().isEmpty()) {
            result.addError("PAYMENT_PLAN_EMPTY", "Payment plan bucket is empty");
        }
        return result;
    }

    /**
     * 将一个金额 bucket 编译成商户费率快照和 PSP 路由候选。
     */
    private PaymentPlanCompileResult.CompiledBucket compileBucket(PaymentPlanCompileRequest request,
                                                                 PaymentPlanAmountRange range,
                                                                 int bucketSort,
                                                                 List<MerchantFeeRuleEntity> merchantRules,
                                                                 List<PaymentRouteRuleEntity> paymentRouteRules,
                                                                 PaymentPlanCompileResult result) {
        // 源配置边界已经被切成 bucket 边界，因此用起始金额即可代表整个 bucket。
        BigDecimal sampleAmount = range.startAmount();
        MerchantFeeRuleEntity merchantRule = merchantRule(request, merchantRules, sampleAmount);
        if (merchantRule == null) {
            result.addError("MERCHANT_FEE_RULE_MISSING", "Merchant fee rule is not configured for amount " + sampleAmount);
        }

        List<PaymentPlanCompileResult.CompiledRouteOption> routeOptionDetails = paymentRouteOptions(request, paymentRouteRules, sampleAmount, result);
        result.getRouteOptionDiagnostics().addAll(routeOptionDetails);
        List<PaymentPlanRouteOptionEntity> routeOptions = routeOptionDetails.stream()
                .map(PaymentPlanCompileResult.CompiledRouteOption::getOption)
                .toList();
        if (routeOptions.isEmpty()) {
            result.addError("PAYMENT_ROUTE_OPTION_MISSING", "No available payment route option for amount " + sampleAmount);
        }
        if (merchantRule == null || routeOptions.isEmpty()) {
            return null;
        }

        PaymentPlanBucketEntity bucket = new PaymentPlanBucketEntity();
        bucket.setTenantId(request.getTenantId());
        bucket.setBucketStartAmount(range.startAmount());
        bucket.setBucketEndAmount(range.endAmount());
        bucket.setMerchantFeeRuleId(merchantRule.getId());
        bucket.setMerchantFeeSnapshotJson(merchantSnapshotJson(merchantRule));
        bucket.setSort(bucketSort);

        PaymentPlanCompileResult.CompiledBucket compiledBucket = new PaymentPlanCompileResult.CompiledBucket();
        compiledBucket.setBucket(bucket);
        compiledBucket.setMerchantFeeRule(merchantRule);
        compiledBucket.setRouteOptions(routeOptions);
        compiledBucket.setRouteOptionDetails(routeOptionDetails);
        return compiledBucket;
    }

    /**
     * 查找某个 bucket 金额对应的商户费率规则。
     *
     * <p>如果请求指定了 merchantFeeRuleId，则只允许使用该规则；
     * 否则由 DAO 按匹配精确度和优先级选择最优规则。</p>
     */
    private MerchantFeeRuleEntity merchantRule(PaymentPlanCompileRequest request,
                                               List<MerchantFeeRuleEntity> merchantRules,
                                               BigDecimal sampleAmount) {
        if (request.getMerchantFeeRuleId() != null) {
            return merchantRules.stream()
                    .filter(rule -> Objects.equals(rule.getId(), request.getMerchantFeeRuleId()))
                    .filter(rule -> AmountRangeUtils.contains(rule.getMinAmount(), rule.getMaxAmount(), sampleAmount))
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

    /**
     * 将 payment_route_rule -> payment_route_group -> payment_route_channel
     * 解析成某个 bucket 金额下可用的 PSP 运行时路由候选。
     */
    private List<PaymentPlanCompileResult.CompiledRouteOption> paymentRouteOptions(PaymentPlanCompileRequest request,
                                                                                  List<PaymentRouteRuleEntity> routeRules,
                                                                                  BigDecimal sampleAmount,
                                                                                  PaymentPlanCompileResult result) {
        // 路由规则列表已经排好序，命中金额的第一条规则生效。
        PaymentRouteRuleEntity routeRule = matchingPaymentRouteRules(routeRules, sampleAmount).stream()
                .findFirst()
                .orElse(null);
        if (routeRule == null) {
            result.addError("PAYMENT_ROUTE_RULE_MISSING", "No available payment route rule for amount " + sampleAmount);
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
                        // 配置表中 max_amount <= 0 表示不限制最大金额。
                        .and(item -> item.ge("max_amount", sampleAmount).or().isNull("max_amount").or().le("max_amount", BigDecimal.ZERO))
                        .orderByAsc("priority")
                        .orderByAsc("fallback_order")
                        .orderByAsc("id"))
                .stream()
                .filter(channel -> AmountRangeUtils.contains(channel.getMinAmount(), channel.getMaxAmount(), sampleAmount))
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

    /**
     * 根据路由通道和 PSP 资源构建一条 payment_plan_route_option。
     */
    private PaymentPlanCompileResult.CompiledRouteOption routeOption(PaymentPlanCompileRequest request,
                                                                     PaymentRouteRuleEntity routeRule,
                                                                     PaymentRouteGroupEntity group,
                                                                     PaymentRouteChannelEntity channel,
                                                                     BigDecimal sampleAmount,
                                                                     PaymentPlanCompileResult result) {
        // 路由通道只存资源 id；发布后的候选会保留快照，方便审计和回滚排查。
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
        if (!AmountRangeUtils.contains(method.getMinAmount(), method.getMaxAmount(), sampleAmount)) {
            result.addWarning("PSP_METHOD_AMOUNT_NOT_MATCH", "PSP method amount range does not cover amount " + sampleAmount + " for payment route channel " + channel.getId());
            return null;
        }
        if (!routeChannelMatches(group, channel, method, account)) {
            result.addWarning("PAYMENT_ROUTE_CHANNEL_INVALID", "Payment route channel does not match route group " + group.getId());
            return null;
        }

        // 银行卡代付不在这里按 bank_code 做路由，只通过银行映射判断 PSP 是否支持。
        if (requiresBankMapping(request) && !hasAnyBankMapping(request, channel.getPspId())) {
            result.addWarning("PSP_BANK_MAPPING_MISSING", "PSP bank mapping is not configured for PSP " + channel.getPspId());
            return null;
        }

        PspFeeRuleEntity feeRule = pspFeeRule(request, group, method, channel, sampleAmount, result);
        if (feeRule == null) {
            result.addWarning("PSP_FEE_RULE_MISSING", "PSP fee rule is not configured for payment route channel " + channel.getId());
            // PSP 成本费率默认只是成本侧告警，只有请求明确要求时才阻断编译。
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
        option.setPriority(defaultPriority(channel.getPriority()));
        option.setWeight(defaultPriority(channel.getWeight()));
        option.setFallbackOrder(defaultPriority(channel.getFallbackOrder()));
        option.setStatus(PaymentPlanRouteOptionStatus.ACTIVE);
        option.setSort(defaultPriority(channel.getPriority()));

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

    /**
     * 使用编译出的内存方案执行预览测试用例。
     */
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

    /**
     * 模拟一笔商户订单完成 bucket 匹配、路由选择和费率计算。
     */
    private PaymentPlanCompileResult.TestResult testCase(PaymentPlanCompileRequest request,
                                                        List<PaymentPlanBucket> planBuckets,
                                                        PaymentPlanCompileRequest.TestCase testCase) {
        PaymentPlanCompileResult.TestResult testResult = new PaymentPlanCompileResult.TestResult();
        testResult.setMerchantOrderId(testCase.getMerchantOrderId());
        testResult.setAmount(testCase.getAmount());
        testResult.setBankCode(normalize(testCase.getBankCode()));
        try {
            PaymentPlanBucket bucket = PaymentPlanBucketMatcher.matchPlanBucket(planBuckets, testCase.getAmount());
            // 使用与运行时一致的选择输入：订单号用于稳定权重路由，银行映射用于过滤可用通道。
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

    /**
     * 重新加载 PSP 成本规则，用于预览测试里的成本计算。
     */
    private PspFeeRuleEntity pspFeeRule(PaymentPlanRouteOptionEntity option) {
        return pspFeeRuleDao.selectById(option.getPspFeeRuleId());
    }

    /**
     * 查找某个通道和 bucket 金额对应的 PSP 成本规则。
     *
     * <p>路由通道可以固定 psp_fee_rule_id；如果没有固定，则由 DAO 按 PSP、
     * 账户、方法和请求维度选择最优成本规则。</p>
     */
    private PspFeeRuleEntity pspFeeRule(PaymentPlanCompileRequest request,
                                        PaymentRouteGroupEntity group,
                                        PspMethodEntity method,
                                        PaymentRouteChannelEntity channel,
                                        BigDecimal sampleAmount,
                                        PaymentPlanCompileResult result) {
        if (channel.getPspFeeRuleId() != null) {
            PspFeeRuleEntity feeRule = pspFeeRuleDao.selectById(channel.getPspFeeRuleId());
            if (!pspFeeRuleMatches(request, group, method, channel, sampleAmount, feeRule)) {
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
                effectiveCountryCode(request, group, method),
                request.getCurrency(),
                request.getMethodCode(),
                sampleAmount,
                request.getDirection(),
                Instant.now(),
                StatusEnum.NORMAL.code()
        );
    }

    /**
     * 校验固定的 PSP 成本规则是否可以写入方案快照。
     */
    private boolean pspFeeRuleMatches(PaymentPlanCompileRequest request,
                                      PaymentRouteGroupEntity group,
                                      PspMethodEntity method,
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
        String countryCode = effectiveCountryCode(request, group, method);
        if (StringUtils.isBlank(countryCode) && StringUtils.isNotBlank(feeRule.getCountryCode())) {
            return false;
        }
        if (StringUtils.isNotBlank(countryCode)
                && StringUtils.isNotBlank(feeRule.getCountryCode())
                && !StringUtils.equalsIgnoreCase(StringUtils.trim(countryCode), StringUtils.trim(feeRule.getCountryCode()))) {
            return false;
        }
        return AmountRangeUtils.contains(feeRule.getMinAmount(), feeRule.getMaxAmount(), sampleAmount);
    }

    /**
     * 预览测试用例使用的银行卡过滤逻辑。
     */
    private boolean testBankSupported(PaymentPlanCompileRequest request, PaymentPlanRouteOptionEntity option, String bankCode) {
        if (!requiresBankMapping(request) || StringUtils.isBlank(bankCode)) {
            return !requiresBankMapping(request);
        }
        return bankMapping(request, option, bankCode).isPresent();
    }

    /**
     * 查找银行卡代付测试用例对应的 PSP 银行编码映射。
     */
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

    /**
     * 加载与本次发布金额范围有交集的商户费率规则。
     */
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
                // 配置中的 max_amount <= 0 表示无上限，因此与任意请求最小金额都有交集。
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount").or().le("max_amount", BigDecimal.ZERO))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"));
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.and(item -> item.eq("country_code", request.getCountryCode()).or().isNull("country_code").or().eq("country_code", ""));
        }
        if (request.getMerchantFeeRuleId() != null) {
            wrapper.eq("id", request.getMerchantFeeRuleId());
        }
        return merchantFeeRuleDao.selectList(wrapper);
    }

    /**
     * 加载与本次发布金额范围有交集的支付路由规则。
     */
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
                // 配置中的 max_amount <= 0 表示无上限，因此与任意请求最小金额都有交集。
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount").or().le("max_amount", BigDecimal.ZERO))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"))
                .orderByAsc("priority")
                .orderByAsc("id");
        if (StringUtils.isNotBlank(request.getCountryCode())) {
            wrapper.and(item -> item.eq("country_code", request.getCountryCode()).or().isNull("country_code").or().eq("country_code", ""));
        }
        return paymentRouteRuleDao.selectList(wrapper).stream()
                .sorted(paymentRouteOrder(request))
                .toList();
    }

    /**
     * 加载所有命中路由组下的可用路由通道。
     */
    private List<PaymentRouteChannelEntity> paymentRouteChannels(List<PaymentRouteRuleEntity> routeRules) {
        List<Long> groupIds = routeRules.stream()
                .map(PaymentRouteRuleEntity::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        Long tenantId = routeRules.getFirst().getTenantId();
        return paymentRouteChannelDao.selectList(new QueryWrapper<PaymentRouteChannelEntity>()
                .eq("tenant_id", tenantId)
                .in("group_id", groupIds)
                .eq("status", StatusEnum.NORMAL.code()));
    }

    /**
     * 加载可用路由组，用于预览展示和路由一致性校验。
     */
    private List<PaymentRouteGroupEntity> paymentRouteGroups(List<PaymentRouteRuleEntity> routeRules) {
        List<Long> groupIds = routeRules.stream()
                .map(PaymentRouteRuleEntity::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        Long tenantId = routeRules.getFirst().getTenantId();
        return paymentRouteGroupDao.selectList(new QueryWrapper<PaymentRouteGroupEntity>()
                .eq("tenant_id", tenantId)
                .in("id", groupIds)
                .eq("status", StatusEnum.NORMAL.code()));
    }

    /**
     * 加载路由通道引用到的 PSP 方法。
     */
    private List<PspMethodEntity> pspMethods(List<PaymentRouteChannelEntity> channels) {
        List<Long> methodIds = channels.stream()
                .map(PaymentRouteChannelEntity::getPspMethodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (methodIds.isEmpty()) {
            return List.of();
        }
        return pspMethodDao.selectList(new QueryWrapper<PspMethodEntity>()
                .in("id", methodIds)
                .eq("status", StatusEnum.NORMAL.code()));
    }

    /**
     * 使用具体 bucket 金额过滤已加载的路由规则。
     */
    private List<PaymentRouteRuleEntity> matchingPaymentRouteRules(List<PaymentRouteRuleEntity> routeRules, BigDecimal amount) {
        return routeRules.stream()
                .filter(rule -> AmountRangeUtils.contains(rule.getMinAmount(), rule.getMaxAmount(), amount))
                .toList();
    }

    /**
     * 加载命中路由通道相关的 PSP 成本规则。
     *
     * <p>如果通道固定了 pspFeeRuleId，则直接读取固定规则；否则按通道对应
     * PSP、账户、方法以及解析后的国家/币种/支付方式匹配成本规则。</p>
     */
    private List<PspFeeRuleEntity> pspFeeRules(PaymentPlanCompileRequest request,
                                               List<PaymentRouteGroupEntity> groups,
                                               List<PaymentRouteChannelEntity> channels,
                                               List<PspMethodEntity> methods) {
        Map<Long, PaymentRouteGroupEntity> groupMap = routeGroupMap(groups);
        Map<Long, PspMethodEntity> methodMap = pspMethodMap(methods);
        Map<Long, PspFeeRuleEntity> feeRuleMap = new LinkedHashMap<>();
        for (PaymentRouteChannelEntity channel : channels) {
            if (channel.getPspFeeRuleId() != null) {
                PspFeeRuleEntity feeRule = pspFeeRuleDao.selectById(channel.getPspFeeRuleId());
                if (feeRule != null) {
                    feeRuleMap.put(feeRule.getId(), feeRule);
                }
                continue;
            }
            PaymentRouteGroupEntity group = groupMap.get(channel.getGroupId());
            PspMethodEntity method = methodMap.get(channel.getPspMethodId());
            for (PspFeeRuleEntity feeRule : pspFeeRulesForChannel(request, group, method, channel)) {
                feeRuleMap.put(feeRule.getId(), feeRule);
            }
        }
        return new ArrayList<>(feeRuleMap.values());
    }

    private List<PspFeeRuleEntity> pspFeeRulesForChannel(PaymentPlanCompileRequest request,
                                                         PaymentRouteGroupEntity group,
                                                         PspMethodEntity method,
                                                         PaymentRouteChannelEntity channel) {
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<PspFeeRuleEntity>()
                .eq("tenant_id", request.getTenantId())
                .eq("psp_id", channel.getPspId())
                .eq("direction", request.getDirection())
                .eq("currency", request.getCurrency())
                .eq("status", StatusEnum.NORMAL.code())
                .and(item -> item.eq("psp_account_id", channel.getPspAccountId()).or().isNull("psp_account_id"))
                .and(item -> item.eq("psp_method_id", channel.getPspMethodId()).or().isNull("psp_method_id"))
                .and(item -> item.eq("method_code", request.getMethodCode()).or().isNull("method_code").or().eq("method_code", ""))
                .and(item -> item.le("min_amount", request.getMaxAmount()).or().isNull("min_amount"))
                // 配置中的 max_amount <= 0 表示无上限，因此与任意请求最小金额都有交集。
                .and(item -> item.ge("max_amount", request.getMinAmount()).or().isNull("max_amount").or().le("max_amount", BigDecimal.ZERO))
                .and(item -> item.le("effective_at", Instant.now()).or().isNull("effective_at"))
                .and(item -> item.gt("expire_at", Instant.now()).or().isNull("expire_at"));
        String countryCode = effectiveCountryCode(request, group, method);
        if (StringUtils.isNotBlank(countryCode)) {
            wrapper.and(item -> item.eq("country_code", countryCode).or().isNull("country_code").or().eq("country_code", ""));
        } else {
            wrapper.and(item -> item.isNull("country_code").or().eq("country_code", ""));
        }
        return pspFeeRuleDao.selectList(wrapper);
    }

    private Map<Long, PaymentRouteGroupEntity> routeGroupMap(List<PaymentRouteGroupEntity> groups) {
        Map<Long, PaymentRouteGroupEntity> result = new LinkedHashMap<>();
        for (PaymentRouteGroupEntity group : groups) {
            if (group.getId() != null) {
                result.put(group.getId(), group);
            }
        }
        return result;
    }

    private Map<Long, PspMethodEntity> pspMethodMap(List<PspMethodEntity> methods) {
        Map<Long, PspMethodEntity> result = new LinkedHashMap<>();
        for (PspMethodEntity method : methods) {
            if (method.getId() != null) {
                result.put(method.getId(), method);
            }
        }
        return result;
    }

    private String effectiveCountryCode(PaymentPlanCompileRequest request,
                                        PaymentRouteGroupEntity group,
                                        PspMethodEntity method) {
        if (group != null && StringUtils.isNotBlank(group.getCountryCode())) {
            return group.getCountryCode();
        }
        if (method != null && StringUtils.isNotBlank(method.getCountryCode())) {
            return method.getCountryCode();
        }
        return request.getCountryCode();
    }

    /**
     * 收集所有可能改变编译结果的源配置金额边界。
     */
    private List<PaymentPlanAmountRange> sourceRanges(List<MerchantFeeRuleEntity> merchantRules,
                                                      List<PaymentRouteRuleEntity> paymentRouteRules,
                                                      List<PaymentRouteChannelEntity> paymentRouteChannels,
                                                      List<PspMethodEntity> pspMethods,
                                                      List<PspFeeRuleEntity> pspFeeRules) {
        List<PaymentPlanAmountRange> ranges = new ArrayList<>();
        merchantRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        paymentRouteRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        paymentRouteChannels.forEach(channel -> ranges.add(PaymentPlanAmountRange.closed(channel.getMinAmount(), channel.getMaxAmount())));
        pspMethods.forEach(method -> ranges.add(PaymentPlanAmountRange.closed(method.getMinAmount(), method.getMaxAmount())));
        pspFeeRules.forEach(rule -> ranges.add(PaymentPlanAmountRange.closed(rule.getMinAmount(), rule.getMaxAmount())));
        return ranges;
    }

    /**
     * 创建本次编译对应的未发布 catalog 元数据。
     */
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

    /**
     * 在查询配置前校验编译请求。
     *
     * <p>配置里的 maxAmount 可以为 0 表示无上限，但请求里的 maxAmount
     * 必须大于 0，因为 payment_plan_bucket 需要一个有限的发布范围。</p>
     */
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

    /**
     * 匹配前统一规范化维度编码，保证路由和费率查询不受大小写影响。
     */
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

    /**
     * 检查 PSP、PSP 方法和 PSP 账户是否可用于当前订单方向。
     */
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

    /**
     * 检查某个 PSP 在当前请求维度下是否配置了银行映射。
     */
    private boolean hasAnyBankMapping(PaymentPlanCompileRequest request, Long pspId) {
        return pspBankMappingDao.selectCount(new QueryWrapper<PspBankMappingEntity>()
                .eq("psp_id", pspId)
                .eq("country_code", request.getCountryCode())
                .eq("currency", request.getCurrency())
                .eq("status", StatusEnum.NORMAL.code())) > 0;
    }

    /**
     * 只有银行卡代付需要银行映射。
     */
    private boolean requiresBankMapping(PaymentPlanCompileRequest request) {
        return PayDirectionEnum.PAYOUT.code().equals(request.getDirection())
                && PaymentMethodCodes.isBankCard(request.getMethodCode());
    }

    /**
     * 路由规则先按匹配精确度排序，再按业务优先级排序。
     */
    private Comparator<PaymentRouteRuleEntity> paymentRouteOrder(PaymentPlanCompileRequest request) {
        return Comparator
                .comparingInt((PaymentRouteRuleEntity rule) -> paymentRouteSpecificity(rule, request))
                .thenComparingInt(rule -> defaultPriority(rule.getPriority()))
                .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId());
    }

    /**
     * 分数越小，表示路由规则越精确。
     */
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

    /**
     * 校验 PSP 方法是否支持当前请求的支付维度。
     */
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

    /**
     * 校验路由规则指向的路由组是否具有相同的支付维度。
     */
    private boolean routeGroupMatches(PaymentRouteRuleEntity rule, PaymentRouteGroupEntity group) {
        if (rule == null || group == null) {
            return false;
        }
        return Objects.equals(rule.getTenantId(), group.getTenantId())
                && StatusEnum.NORMAL.code().equals(group.getStatus())
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getDirection()), StringUtils.trim(group.getDirection()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getCountryCode()), StringUtils.trim(group.getCountryCode()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getCurrency()), StringUtils.trim(group.getCurrency()))
                && StringUtils.equalsIgnoreCase(StringUtils.trim(rule.getMethodCode()), StringUtils.trim(group.getMethodCode()));
    }

    /**
     * 校验路由通道中的 PSP 资源是否与所属路由组一致。
     */
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

    /**
     * 快照运行时 bucket 使用的商户费率数据。
     */
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

    /**
     * 快照运行时路由候选使用的 PSP 成本费率数据。
     */
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

    /**
     * 快照命中路由组的路由规则。
     */
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

    /**
     * 快照路由组/通道池元数据。
     */
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

    /**
     * 快照路由候选使用的路由通道。
     */
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

    /**
     * 快照路由候选使用的 PSP 数据。
     */
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

    /**
     * 快照路由候选使用的 PSP 方法数据。
     */
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

    /**
     * 快照路由候选使用的 PSP 账户数据。
     */
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

    /**
     * 生成轻量级源配置指纹，用于比较编译后的方案内容是否变化。
     */
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

    /**
     * 给写入运行时候选的优先级类字段补默认值。
     */
    private int defaultPriority(Integer value) {
        return value == null ? DEFAULT_PRIORITY : value;
    }

    /**
     * 将 BigDecimal 写入快照时避免科学计数法。
     */
    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * 规范化支付维度编码，用于大小写不敏感匹配。
     */
    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
