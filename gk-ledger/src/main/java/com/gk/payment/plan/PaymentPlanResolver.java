package com.gk.payment.plan;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.constant.PaymentMethodCodes;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PaymentPlanBucketEntity;
import com.gk.payment.entity.PaymentPlanCatalogEntity;
import com.gk.payment.entity.PaymentPlanRouteOptionEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.fee.MerchantFeeAmount;
import com.gk.payment.fee.MerchantFeeCalculator;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.psp.callback.support.PspCallbackUrlBuilder;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.fee.PspFeeCalculator;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.route.PspRouteResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 支付决策表运行时解析器。
 * <p>
 * 这里只读取已经发布的 ACTIVE catalog，不做后台配置编译，方便后续管理后台和商户 API 分开部署。
 */
@Service
@RequiredArgsConstructor
public class PaymentPlanResolver {
    private static final long RESOURCE_CACHE_TTL_MILLIS = Duration.ofSeconds(30).toMillis();

    private final PaymentPlanCacheService paymentPlanCacheService;
    private final RedisUtils redisUtils;
    private final PspProviderDao pspProviderDao;
    private final PspMethodDao pspMethodDao;
    private final PspAccountDao pspAccountDao;
    private final PspBankMappingDao pspBankMappingDao;
    private final PspCallbackUrlBuilder callbackUrlBuilder;
    private final ConcurrentHashMap<Long, CacheEntry<PspProviderEntity>> providerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspMethodEntity>> methodCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CacheEntry<PspAccountEntity>> accountCache = new ConcurrentHashMap<>();

    public Optional<PaymentPlan> resolvePayin(PayOrderEntity order) {
        PaymentPlanKey key = PaymentPlanKey.of(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                PayDirectionEnum.PAYIN.code(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode()
        );
        return resolve(key, order.getAmount(), null, order.getPayOrderNo());
    }

    public Optional<PaymentPlan> resolvePayout(PayoutOrderEntity order) {
        PaymentPlanKey key = PaymentPlanKey.of(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                PayDirectionEnum.PAYOUT.code(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode()
        );
        return resolve(key, order.getAmount(), order.getPayeeBankCode(), order.getPayoutOrderNo());
    }

    private Optional<PaymentPlan> resolve(PaymentPlanKey key, BigDecimal amount, String bankCode, String seed) {
        return paymentPlanCacheService.findActive(key)
                .map(catalog -> buildPlan(key, catalog.getCatalog(), PaymentPlanBucketMatcher.matchPlanBucket(catalog.getBuckets(), amount), amount, bankCode, seed));
    }

    private PaymentPlan buildPlan(PaymentPlanKey key,
                                  PaymentPlanCatalogEntity catalog,
                                  PaymentPlanBucket bucket,
                                  BigDecimal amount,
                                  String bankCode,
                                  String seed) {
        PaymentPlanBucketEntity bucketEntity = bucket.getBucket();
        PaymentPlanRouteOptionEntity option = selectRouteOption(key, bucket, bankCode, seed);
        PspBankMappingEntity bankMapping = bankMapping(key, option, bankCode).orElse(null);
        MerchantFeeResult merchantFee = merchantFee(bucketEntity, amount);
        PspRouteResult route = route(catalog.getDirection(), option, bankMapping);
        PspFeeResult pspFee = pspFee(option, amount);

        PaymentPlan plan = new PaymentPlan();
        plan.setCatalogId(catalog.getId());
        plan.setCatalogVersion(catalog.getVersion());
        plan.setBucketId(bucketEntity.getId());
        plan.setRouteOptionId(option.getId());
        plan.setMerchantFee(merchantFee);
        plan.setRoute(route);
        plan.setPspFee(pspFee);
        plan.setMerchantFeeAmount(merchantFee.getMerchantFeeAmount());
        plan.setSettleAmount(merchantFee.getSettleAmount());
        plan.setPspFeeAmount(pspFee == null ? BigDecimal.ZERO : pspFee.getPspFeeAmount());
        return plan;
    }

    private PaymentPlanRouteOptionEntity selectRouteOption(PaymentPlanKey key,
                                                           PaymentPlanBucket bucket,
                                                           String bankCode,
                                                           String seed) {
        return PaymentPlanRouteOptionSelector.select(
                bucket.getRouteOptions(),
                seed,
                Collections.emptySet(),
                Collections.emptySet(),
                option -> optionRuntimeAvailable(key, option, bankCode)
        );
    }

    private MerchantFeeResult merchantFee(PaymentPlanBucketEntity bucket, BigDecimal amount) {
        MerchantFeeRuleEntity rule = merchantFeeRule(bucket);
        MerchantFeeAmount calculated = MerchantFeeCalculator.calculate(amount, rule);

        MerchantFeeResult result = new MerchantFeeResult();
        result.setRule(rule);
        result.setMerchantFeeAmount(calculated.feeAmount());
        result.setSettleAmount(calculated.payinSettleAmount());
        result.setSnapshotJson(bucket.getMerchantFeeSnapshotJson());
        return result;
    }

    private MerchantFeeRuleEntity merchantFeeRule(PaymentPlanBucketEntity bucket) {
        JSONObject json = parseSnapshot(bucket.getMerchantFeeSnapshotJson(), "Merchant fee snapshot is not configured");
        MerchantFeeRuleEntity rule = new MerchantFeeRuleEntity();
        rule.setId(bucket.getMerchantFeeRuleId());
        rule.setRuleName(json.getString("ruleName"));
        rule.setDirection(json.getString("direction"));
        rule.setCountryCode(json.getString("countryCode"));
        rule.setCurrency(json.getString("currency"));
        rule.setMethodCode(json.getString("methodCode"));
        rule.setFeeMode(json.getString("feeMode"));
        rule.setFeeRate(decimal(json, "feeRate"));
        rule.setFeeFixed(decimal(json, "feeFixed"));
        rule.setMinFee(decimal(json, "minFee"));
        rule.setMaxFee(decimal(json, "maxFee"));
        rule.setFeeBearer(json.getString("feeBearer"));
        rule.setSettleMode(json.getString("settleMode"));
        return rule;
    }

    private PspFeeResult pspFee(PaymentPlanRouteOptionEntity option, BigDecimal amount) {
        if (option.getPspFeeRuleId() == null || StringUtils.isBlank(option.getPspFeeSnapshotJson())) {
            return null;
        }
        PspFeeRuleEntity rule = pspFeeRule(option);
        BigDecimal feeAmount = PspFeeCalculator.calculate(amount, rule);

        PspFeeResult result = new PspFeeResult();
        result.setRule(rule);
        result.setPspFeeAmount(feeAmount);
        result.setSnapshotJson(option.getPspFeeSnapshotJson());
        return result;
    }

    private PspFeeRuleEntity pspFeeRule(PaymentPlanRouteOptionEntity option) {
        JSONObject json = parseSnapshot(option.getPspFeeSnapshotJson(), "PSP fee snapshot is not configured");
        PspFeeRuleEntity rule = new PspFeeRuleEntity();
        rule.setId(option.getPspFeeRuleId());
        rule.setRuleName(json.getString("ruleName"));
        rule.setPspId(option.getPspId());
        rule.setPspAccountId(option.getPspAccountId());
        rule.setPspMethodId(option.getPspMethodId());
        rule.setDirection(json.getString("direction"));
        rule.setCountryCode(json.getString("countryCode"));
        rule.setCurrency(json.getString("currency"));
        rule.setMethodCode(json.getString("methodCode"));
        rule.setFeeMode(json.getString("feeMode"));
        rule.setFeeRate(decimal(json, "feeRate"));
        rule.setFeeFixed(decimal(json, "feeFixed"));
        rule.setMinFee(decimal(json, "minFee"));
        rule.setMaxFee(decimal(json, "maxFee"));
        return rule;
    }

    private PspRouteResult route(String direction, PaymentPlanRouteOptionEntity option, PspBankMappingEntity bankMapping) {
        PspProviderEntity provider = requireProvider(option.getPspId(), direction);
        PspMethodEntity method = requireMethod(option.getPspMethodId());
        PspAccountEntity account = requirePspAccount(option.getPspAccountId());

        PspRouteResult result = new PspRouteResult();
        result.setRouteRuleId(option.getRouteRuleId());
        result.setRouteGroupId(option.getRouteGroupId());
        result.setRouteChannelId(option.getRouteChannelId());
        result.setPspId(provider.getId());
        result.setPspCode(provider.getPspCode());
        result.setPspBaseUrl(provider.getBaseUrl());
        result.setProviderConfigJson(provider.getConfigJson());
        result.setPspCallbackUrl(platformCallbackUrl(provider.getPspCode(), direction));
        result.setPspMethodId(method.getId());
        result.setPspMethodCode(method.getPspMethodCode());
        result.setMethodConfigJson(method.getConfigJson());
        result.setPspAccountId(account.getId());
        result.setPspAccountNo(account.getPspAccountNo());
        // PSP 密钥只从账户表读取，不进入 payment_plan_bucket 和 Redis。
        result.setPspAccountApiKey(account.getApiKey());
        result.setPspAccountApiSecret(account.getApiSecret());
        result.setAccountConfigJson(account.getConfigJson());
        result.setPspBankCode(bankMapping == null ? null : bankMapping.getPspBankCode());
        return result;
    }

    private boolean optionRuntimeAvailable(PaymentPlanKey key, PaymentPlanRouteOptionEntity option, String bankCode) {
        if (redisUnavailable(key, option)) {
            return false;
        }
        if (!resourceAvailable(key.direction(), option)) {
            return false;
        }
        return bankSupported(key, option, bankCode);
    }

    private boolean resourceAvailable(String direction, PaymentPlanRouteOptionEntity option) {
        PspProviderEntity provider = cached(providerCache, option.getPspId(), pspProviderDao::selectById);
        PspMethodEntity method = cached(methodCache, option.getPspMethodId(), pspMethodDao::selectById);
        PspAccountEntity account = cached(accountCache, option.getPspAccountId(), pspAccountDao::selectById);
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

    private boolean redisUnavailable(PaymentPlanKey key, PaymentPlanRouteOptionEntity option) {
        try {
            return redisUtils.isKeyExist(RedisKeys.getPaymentPspDisableKey(key.tenantId(), key.direction(), option.getPspId()))
                    || redisUtils.isKeyExist(RedisKeys.getPaymentPspAccountDisableKey(key.tenantId(), key.direction(), option.getPspAccountId()))
                    || redisHealthDown(RedisKeys.getPaymentPspHealthKey(key.tenantId(), key.direction(), option.getPspId()))
                    || redisHealthDown(RedisKeys.getPaymentPspAccountHealthKey(key.tenantId(), key.direction(), option.getPspAccountId()));
        } catch (Exception ex) {
            // Redis 状态层不可用时只降级，不阻断交易；最终可用性仍由 DB 状态校验兜底。
            return false;
        }
    }

    private boolean redisHealthDown(String key) {
        Object value = redisUtils.get(key);
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).toUpperCase(java.util.Locale.ROOT);
        return text.contains("DOWN");
    }

    private Optional<PspBankMappingEntity> bankMapping(PaymentPlanKey key, PaymentPlanRouteOptionEntity option, String bankCode) {
        if (!requiresBankMapping(key) || StringUtils.isBlank(bankCode)) {
            return Optional.empty();
        }
        PspBankMappingEntity mapping = pspBankMappingDao.selectOne(new QueryWrapper<PspBankMappingEntity>()
                .eq("psp_id", option.getPspId())
                .eq("country_code", key.countryCode())
                .eq("currency", key.currency())
                .eq("bank_code", normalize(bankCode))
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
        return Optional.ofNullable(mapping).filter(item -> StringUtils.isNotBlank(item.getPspBankCode()));
    }

    private boolean bankSupported(PaymentPlanKey key, PaymentPlanRouteOptionEntity option, String bankCode) {
        // 只有银行卡代付才需要用商户侧银行编码过滤 PSP 路由。
        if (!requiresBankMapping(key)) {
            return true;
        }
        if (StringUtils.isBlank(bankCode)) {
            return false;
        }
        return bankMapping(key, option, bankCode).isPresent();
    }

    private boolean requiresBankMapping(PaymentPlanKey key) {
        return PayDirectionEnum.PAYOUT.code().equals(key.direction())
                && PaymentMethodCodes.isBankCard(key.methodCode());
    }

    private String platformCallbackUrl(String pspCode, String direction) {
        if (PayDirectionEnum.PAYOUT.code().equals(direction)) {
            return callbackUrlBuilder.payoutCallbackUrl(pspCode);
        }
        return callbackUrlBuilder.payCallbackUrl(pspCode);
    }

    private PspProviderEntity requireProvider(Long pspId, String direction) {
        PspProviderEntity provider = cached(providerCache, pspId, pspProviderDao::selectById);
        if (provider == null || !StatusEnum.NORMAL.code().equals(provider.getStatus())) {
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
        PspMethodEntity method = cached(methodCache, pspMethodId, pspMethodDao::selectById);
        if (method == null || !StatusEnum.NORMAL.code().equals(method.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP method is not available");
        }
        return method;
    }

    private PspAccountEntity requirePspAccount(Long pspAccountId) {
        PspAccountEntity account = cached(accountCache, pspAccountId, pspAccountDao::selectById);
        if (account == null || !StatusEnum.NORMAL.code().equals(account.getStatus())) {
            throw new ApiException(ApiErrorCode.UNSUPPORTED_METHOD, "PSP account is not available");
        }
        return account;
    }

    private <T> T cached(ConcurrentHashMap<Long, CacheEntry<T>> cache, Long id, Function<Long, T> loader) {
        if (id == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        CacheEntry<T> cached = cache.get(id);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.value();
        }
        T value = loader.apply(id);
        if (value != null) {
            cache.put(id, new CacheEntry<>(value, now + RESOURCE_CACHE_TTL_MILLIS));
        } else {
            cache.remove(id);
        }
        return value;
    }

    private JSONObject parseSnapshot(String snapshotJson, String message) {
        if (StringUtils.isBlank(snapshotJson)) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, message);
        }
        return JSONObject.parseObject(snapshotJson);
    }

    private BigDecimal decimal(JSONObject json, String field) {
        String value = json.getString(field);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private record CacheEntry<T>(T value, long expiresAtMillis) {
    }
}
