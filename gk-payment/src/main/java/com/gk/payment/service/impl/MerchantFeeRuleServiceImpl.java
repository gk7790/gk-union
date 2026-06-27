package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.common.amount.AmountRangeUtils;
import com.gk.common.amount.FeeLimitUtils;
import com.gk.payment.dao.MerchantFeeRuleDao;
import com.gk.payment.dao.PaymentMethodDao;
import com.gk.payment.dto.MerchantFeeRuleDTO;
import com.gk.payment.dto.MerchantFeeViewResponse;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.entity.PaymentMethodEntity;
import com.gk.payment.fee.MerchantFeeAmount;
import com.gk.payment.fee.MerchantFeeCalculator;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.service.MerchantFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantFeeRuleServiceImpl extends CrudServiceImpl<MerchantFeeRuleDao, MerchantFeeRuleEntity, MerchantFeeRuleDTO> implements MerchantFeeRuleService {
    private static final String SETTLE_MODE_DEDUCT = "DEDUCT";
    private static final String SETTLE_MODE_ADD = "ADD";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaymentMethodDao paymentMethodDao;

    @Autowired
    private PayinPlanCache payinPlanCache;
    @Autowired
    private PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public PageData<MerchantFeeRuleDTO> page(DynMap params) {
        normalizePageParams(params);
        params.put("showTenantName", ReqContextHolder.isPlatform());
        long pageNo = Math.max(params.getLong(Constant.PAGE, 1L), 1L);
        long limit = Math.max(params.getLong(Constant.LIMIT, 10L), 1L);
        params.put("offset", (pageNo - 1) * limit);
        params.put("limitValue", limit);

        Long total = baseDao.countPageWithName(params);
        List<MerchantFeeRuleDTO> list = total == null || total == 0L
                ? List.of()
                : baseDao.selectPageWithName(params);
        return new PageData<>(list, total == null ? 0L : total);
    }

    @Override
    public MerchantFeeViewResponse merchantView(DynMap params) {
        DynMap query = params == null ? DynMap.empty() : params;
        normalizePageParams(query);
        FeeViewScope scope = feeViewScope(query);
        List<MerchantFeeRuleEntity> rules = merchantFeeRulesForView(query, scope);
        Map<String, List<PaymentMethodEntity>> methodMap = paymentMethods(rules);
        String requestCountryCode = normalizeBlankToNull(query.getStr("countryCode"));

        MerchantFeeViewResponse response = new MerchantFeeViewResponse();
        response.setTenantId(scope.tenantId());
        response.setMerchantId(scope.merchantId());
        response.setCountryCode(requestCountryCode);
        response.setCurrency(normalizeBlankToNull(query.getStr("currency")));

        Map<String, MerchantFeeViewResponse.Group> groupMap = new LinkedHashMap<>();
        uniqueMerchantFeeRules(rules, requestCountryCode).stream()
                .sorted(Comparator
                        .comparingInt((MerchantFeeRuleEntity rule) -> directionSort(rule.getDirection()))
                        .thenComparing(rule -> StringUtils.defaultString(rule.getMethodCode()))
                        .thenComparing(rule -> rule.getMinAmount() == null ? BigDecimal.ZERO : rule.getMinAmount())
                        .thenComparing(rule -> rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority())
                        .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId()))
                .forEach(rule -> {
                    String direction = normalize(rule.getDirection());
                    MerchantFeeViewResponse.Group group = groupMap.computeIfAbsent(direction, key -> group(direction));
                    group.getItems().add(feeViewItem(rule, methodMap.getOrDefault(normalize(rule.getMethodCode()), List.of())));
                });
        response.setGroups(new ArrayList<>(groupMap.values()));
        return response;
    }

    private FeeViewScope feeViewScope(DynMap params) {
        ReqContext context = ReqContextHolder.get();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        if (ReqContextHolder.isPlatform()) {
            return requireFeeViewScope(tenantId, merchantId);
        }
        if (SubjectTypeEnum.TENANT.matches(context.getSubjectType())) {
            return requireFeeViewScope(context.getTenantId(), merchantId);
        }
        if (SubjectTypeEnum.MERCHANT.matches(context.getSubjectType())) {
            return requireFeeViewScope(context.getTenantId(), context.getMerchantId());
        }
        if (context.getTenantId() != null && context.getMerchantId() != null) {
            return requireFeeViewScope(context.getTenantId(), context.getMerchantId());
        }
        return requireFeeViewScope(tenantId, merchantId);
    }

    private FeeViewScope requireFeeViewScope(Long tenantId, Long merchantId) {
        if (tenantId == null || tenantId <= 0 || merchantId == null || merchantId <= 0) {
            throw new GkException(ErrorCode.BAD_REQUEST, "tenantId and merchantId are required");
        }
        return new FeeViewScope(tenantId, merchantId);
    }

    private List<MerchantFeeRuleEntity> merchantFeeRulesForView(DynMap params, FeeViewScope scope) {
        String direction = normalizeBlankToNull(params.getStr("direction"));
        String countryCode = normalizeBlankToNull(params.getStr("countryCode"));
        String currency = normalizeBlankToNull(params.getStr("currency"));
        String methodCode = normalizeBlankToNull(params.getStr("methodCode"));
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", scope.tenantId());
        wrapper.eq("merchant_id", scope.merchantId());
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq(StringUtils.isNotBlank(direction), "direction", direction);
        wrapper.and(StringUtils.isNotBlank(countryCode), item -> item.eq("country_code", countryCode).or().isNull("country_code").or().eq("country_code", ""));
        wrapper.eq(StringUtils.isNotBlank(currency), "currency", currency);
        wrapper.eq(StringUtils.isNotBlank(methodCode), "method_code", methodCode);
        Instant now = Instant.now();
        wrapper.and(item -> item.le("effective_at", now).or().isNull("effective_at"));
        wrapper.and(item -> item.gt("expire_at", now).or().isNull("expire_at"));
        wrapper.orderByAsc("direction")
                .orderByAsc("method_code")
                .orderByAsc("min_amount")
                .orderByAsc("priority")
                .orderByDesc("id");
        return baseDao.selectList(wrapper);
    }

    private List<MerchantFeeRuleEntity> uniqueMerchantFeeRules(List<MerchantFeeRuleEntity> rules, String requestCountryCode) {
        Map<String, MerchantFeeRuleEntity> selected = new LinkedHashMap<>();
        rules.stream()
                .sorted(merchantFeeViewRuleOrder(requestCountryCode))
                .forEach(rule -> selected.putIfAbsent(feeViewUniqueKey(rule, requestCountryCode), rule));
        return new ArrayList<>(selected.values());
    }

    private Comparator<MerchantFeeRuleEntity> merchantFeeViewRuleOrder(String requestCountryCode) {
        return Comparator
                .comparingInt((MerchantFeeRuleEntity rule) -> countrySpecificity(rule, requestCountryCode))
                .thenComparingInt(rule -> rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority())
                .thenComparing(rule -> rule.getMinAmount() == null ? BigDecimal.ZERO : rule.getMinAmount())
                .thenComparing(rule -> rule.getId() == null ? Long.MAX_VALUE : rule.getId());
    }

    private int countrySpecificity(MerchantFeeRuleEntity rule, String requestCountryCode) {
        if (StringUtils.isBlank(requestCountryCode)) {
            return 0;
        }
        return Strings.CI.equals(StringUtils.trim(rule.getCountryCode()), requestCountryCode) ? 0 : 1;
    }

    private String feeViewUniqueKey(MerchantFeeRuleEntity rule, String requestCountryCode) {
        String countryKey = StringUtils.isBlank(requestCountryCode) ? normalize(rule.getCountryCode()) : requestCountryCode;
        return normalize(rule.getDirection())
                + "|" + normalize(rule.getMethodCode())
                + "|" + countryKey;
    }

    private Map<String, List<PaymentMethodEntity>> paymentMethods(List<MerchantFeeRuleEntity> rules) {
        Set<String> methodCodes = rules.stream()
                .map(MerchantFeeRuleEntity::getMethodCode)
                .filter(StringUtils::isNotBlank)
                .map(this::normalize)
                .collect(Collectors.toSet());
        if (methodCodes.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<PaymentMethodEntity> wrapper = new QueryWrapper<>();
        wrapper.select("method_code", "method_name", "direction", "country_code", "currency", "status", "sort");
        wrapper.in("method_code", methodCodes);
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.orderByAsc("sort").orderByAsc("method_code");
        return paymentMethodDao.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(method -> normalize(method.getMethodCode()), LinkedHashMap::new, Collectors.toList()));
    }

    private MerchantFeeViewResponse.Group group(String direction) {
        MerchantFeeViewResponse.Group group = new MerchantFeeViewResponse.Group();
        group.setDirection(direction);
        group.setDirectionName(directionName(direction));
        return group;
    }

    private MerchantFeeViewResponse.Item feeViewItem(MerchantFeeRuleEntity rule, List<PaymentMethodEntity> methods) {
        MerchantFeeViewResponse.Item item = new MerchantFeeViewResponse.Item();
        item.setRuleId(rule.getId());
        item.setRuleName(rule.getRuleName());
        item.setDirection(rule.getDirection());
        item.setDirectionName(directionName(rule.getDirection()));
        item.setCountryCode(rule.getCountryCode());
        item.setCurrency(rule.getCurrency());
        item.setMethodCode(rule.getMethodCode());
        item.setMethodName(methodName(rule, methods));
        item.setMinAmount(rule.getMinAmount());
        item.setMaxAmount(rule.getMaxAmount());
        item.setFeeMode(rule.getFeeMode());
        item.setFeeModeName(feeModeName(rule.getFeeMode()));
        item.setFeeRate(rule.getFeeRate());
        item.setFeeRateText(rateText(rule.getFeeRate()));
        item.setFeeFixed(rule.getFeeFixed());
        item.setFeeFixedText(amountText(rule.getFeeFixed()));
        item.setMinFee(rule.getMinFee());
        item.setMinFeeText(rule.getMinFee() == null ? null : amountText(rule.getMinFee()));
        item.setMaxFee(rule.getMaxFee());
        item.setMaxFeeText(rule.getMaxFee() == null ? null : amountText(rule.getMaxFee()));
        item.setFeeText(feeText(rule));
        item.setFeeBearer(rule.getFeeBearer());
        item.setSettleMode(rule.getSettleMode());
        item.setPriority(rule.getPriority());
        item.setEffectiveAt(rule.getEffectiveAt());
        item.setExpireAt(rule.getExpireAt());
        item.setStatus(rule.getStatus());
        item.setRemark(rule.getRemark());
        return item;
    }

    private String methodName(MerchantFeeRuleEntity rule, List<PaymentMethodEntity> methods) {
        return methods.stream()
                .sorted(Comparator
                        .comparingInt((PaymentMethodEntity method) -> methodSpecificity(rule, method))
                        .thenComparing(method -> method.getSort() == null ? Integer.MAX_VALUE : method.getSort()))
                .map(PaymentMethodEntity::getMethodName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(rule.getMethodCode());
    }

    private int methodSpecificity(MerchantFeeRuleEntity rule, PaymentMethodEntity method) {
        int score = 0;
        if (!Strings.CI.equals(StringUtils.trim(rule.getDirection()), StringUtils.trim(method.getDirection()))
                && !"BOTH".equalsIgnoreCase(StringUtils.trim(method.getDirection()))) {
            score += 4;
        }
        if (StringUtils.isNotBlank(method.getCurrency())
                && !Strings.CI.equals(StringUtils.trim(rule.getCurrency()), StringUtils.trim(method.getCurrency()))) {
            score += 2;
        }
        if (StringUtils.isNotBlank(method.getCountryCode())
                && !Strings.CI.equals(StringUtils.trim(rule.getCountryCode()), StringUtils.trim(method.getCountryCode()))) {
            score += 1;
        }
        return score;
    }

    private String feeText(MerchantFeeRuleEntity rule) {
        String feeMode = normalize(rule.getFeeMode());
        return switch (feeMode) {
            case "RATE" -> rateText(rule.getFeeRate());
            case "FIXED" -> amountText(rule.getFeeFixed());
            case "RATE_FIXED" -> rateText(rule.getFeeRate()) + " + " + amountText(rule.getFeeFixed());
            default -> rateText(rule.getFeeRate()) + " + " + amountText(rule.getFeeFixed());
        };
    }

    private String rateText(BigDecimal value) {
        return displayDecimalText(defaultZero(value).multiply(ONE_HUNDRED)) + "%";
    }

    private String amountText(BigDecimal value) {
        return displayDecimalText(defaultZero(value)) + "/";
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String directionName(String direction) {
        if (PayDirectionEnum.PAYIN.matches(direction)) {
            return "代收";
        }
        if (PayDirectionEnum.PAYOUT.matches(direction)) {
            return "代付";
        }
        return direction;
    }

    private String feeModeName(String feeMode) {
        return switch (normalize(feeMode)) {
            case "RATE" -> "比例";
            case "FIXED" -> "固定";
            case "RATE_FIXED" -> "比例+固定";
            default -> feeMode;
        };
    }

    private int directionSort(String direction) {
        if (PayDirectionEnum.PAYIN.matches(direction)) {
            return 0;
        }
        if (PayDirectionEnum.PAYOUT.matches(direction)) {
            return 1;
        }
        return 9;
    }

    private String normalizeBlankToNull(String value) {
        String normalized = normalize(value);
        return StringUtils.isBlank(normalized) ? null : normalized;
    }

    private String displayDecimalText(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private void normalizePageParams(DynMap params) {
        normalizeParam(params, "direction");
        normalizeParam(params, "countryCode");
        normalizeParam(params, "currency");
        normalizeParam(params, "methodCode");
        normalizeParam(params, "feeMode");
        normalizeParam(params, "feeBearer");
    }

    private void normalizeParam(DynMap params, String key) {
        String value = params.getStr(key);
        if (StrUtil.isNotBlank(value)) {
            params.put(key, normalize(value));
        }
    }

    @Override
    public void save(MerchantFeeRuleDTO dto) {
        normalizePersistFields(dto);
        validateAmountRange(dto);
        validateFeeLimit(dto);
        super.save(dto);
        evictPayinPlanCache();
    }

    @Override
    public void update(MerchantFeeRuleDTO dto) {
        normalizePersistFields(dto);
        validateAmountRange(dto);
        validateFeeLimit(dto);
        super.update(dto);
        clearMerchantAppWhenNeeded(dto);
        evictPayinPlanCache();
    }

    private void validateAmountRange(MerchantFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        try {
            AmountRangeUtils.validateConfigRange(dto.getMinAmount(), dto.getMaxAmount());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }

    private void validateFeeLimit(MerchantFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        validateNonNegative(dto.getFeeRate(), "fee_rate cannot be negative");
        validateNonNegative(dto.getFeeFixed(), "fee_fixed cannot be negative");
        try {
            FeeLimitUtils.validateFeeLimit(dto.getMinFee(), dto.getMaxFee());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }

    private void validateNonNegative(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new GkException(ErrorCode.BAD_REQUEST, message);
        }
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPayinPlanCache();
    }

    /**
     * 后台管理页的查询条件组装     * <p>
     * 这里只负责列分页筛选，不参与下单时的费率命中；下单命中规则{@link #selectRule}     */
    @Override
    public QueryWrapper<MerchantFeeRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String feeMode = params.getStr("feeMode");
        String feeBearer = params.getStr("feeBearer");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(feeMode), "fee_mode", normalize(feeMode));
        wrapper.eq(StrUtil.isNotBlank(feeBearer), "fee_bearer", normalize(feeBearer));
        return wrapper;
    }

    /**
     * 计算代收订单的商户手续费     * <p>
     * 代收成功后入账使settleAmount，当前由 {@link MerchantFeeCalculator}
     * 根据手续费承担方计算“订单金- 商户手续费”或保持订单金额不变     */
    @Override
    public MerchantFeeResult calculatePayin(PayOrderEntity order) {
        return calculate(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                null,
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    /**
     * 计算代付订单的商户手续费     * <p>
     * 代付侧调用方会将返回merchantFeeAmount 加到 totalDebitAmount     * 冻结和扣减时按“代付本+ 商户手续费”处理     */
    @Override
    public MerchantFeeResult calculatePayout(PayoutOrderEntity order) {
        return calculate(
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

    private MerchantFeeResult calculate(
            Long tenantId,
            Long merchantId,
            Long merchantAppId,
            String countryCode,
            String currency,
            String methodCode,
            BigDecimal orderAmount,
            String direction
    ) {
        // 先按商户、币种、方向、支付方式、金额区间等条件选中一条有效规则，再交给纯计算器算金额
        MerchantFeeRuleEntity rule = selectRule(tenantId, merchantId, merchantAppId, countryCode, currency, methodCode, orderAmount, direction);
        MerchantFeeAmount amount;
        try {
            amount = MerchantFeeCalculator.calculate(orderAmount, rule);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, ex.getMessage());
        }

        MerchantFeeResult result = new MerchantFeeResult();
        result.setRule(rule);
        result.setMerchantFeeAmount(amount.feeAmount());
        result.setSettleAmount(amount.payinSettleAmount());
        result.setSnapshotJson(toSnapshotJson(rule));
        return result;
    }

    /**
     * 选择下单时真正生效的商户费率规则     * <p>
     * 必须匹配租户、商户、订单方向、币种和启用状态；应用、支付方式、金额区间、生效时间支持空值兜底     * 多条规则同时命中时，优先选择更精确的规则，再priority 数值越小越优先     */
    private MerchantFeeRuleEntity selectRule(
            Long tenantId,
            Long merchantId,
            Long merchantAppId,
            String countryCode,
            String currency,
            String methodCode,
            BigDecimal orderAmount,
            String direction
    ) {
        Instant now = Instant.now();
        MerchantFeeRuleEntity rule = baseDao.selectBestMatchForOrder(
                tenantId,
                merchantId,
                merchantAppId,
                normalize(countryCode),
                normalize(currency),
                normalize(methodCode),
                orderAmount,
                direction,
                now,
                StatusEnum.NORMAL.code()
        );
        if (rule == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Merchant fee rule is not configured");
        }
        return rule;
    }

    /**
     * 生成订单费率快照     * <p>
     * 快照只保留计费和审计需要的字段，不保存整条规则对象，避免后续规则字段变化影响历史订单解析     */
    private String toSnapshotJson(MerchantFeeRuleEntity rule) {
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
     * 使用普通十进制字符串保存金费率，避BigDecimal 科学计数法进JSON 快照     */
    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private void normalizePersistFields(MerchantFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        if (dto.getMerchantAppId() != null && dto.getMerchantAppId() <= 0) {
            dto.setMerchantAppId(null);
        }
        dto.setFeeBearer(null);
        String direction = StringUtils.defaultIfBlank(dto.getDirection(), currentDirection(dto.getId()));
        dto.setSettleMode(PayDirectionEnum.PAYOUT.matches(direction) ? SETTLE_MODE_ADD : SETTLE_MODE_DEDUCT);
    }

    private void clearMerchantAppWhenNeeded(MerchantFeeRuleDTO dto) {
        if (dto == null || dto.getId() == null || dto.getMerchantAppId() != null) {
            return;
        }
        MerchantFeeRuleEntity update = new MerchantFeeRuleEntity();
        update.setMerchantAppId(null);
        baseDao.update(update, new UpdateWrapper<MerchantFeeRuleEntity>()
                .eq("id", dto.getId())
                .set("merchant_app_id", null));
    }

    private String currentDirection(Long id) {
        if (id == null) {
            return null;
        }
        MerchantFeeRuleEntity entity = baseDao.selectById(id);
        return entity == null ? null : entity.getDirection();
    }

    /**
     * 统一将配置编码和请求编码转换成大写后比较，降低前端输入大小写差异带来的匹配失败     */
    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPayinPlanCache() {
        // 商户费率会影PayinPlan 的手续费和结算金额，变更后必须清空缓存
                if (payinPlanCache != null) {
            payinPlanCache.evictAll();
        }
        if (paymentPlanCacheService != null) {
            paymentPlanCacheService.evictAll();
        }
    }

    private record FeeViewScope(Long tenantId, Long merchantId) {
    }
}
