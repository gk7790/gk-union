package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.dao.MerchantFeeRuleDao;
import com.gk.payment.dto.MerchantFeeRuleDTO;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.fee.MerchantFeeAmount;
import com.gk.payment.fee.MerchantFeeCalculator;
import com.gk.payment.fee.MerchantFeeResult;
import com.gk.payment.service.MerchantFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantFeeRuleServiceImpl extends CrudServiceImpl<MerchantFeeRuleDao, MerchantFeeRuleEntity, MerchantFeeRuleDTO> implements MerchantFeeRuleService {
    private static final String ORDER_TYPE_PAYIN = "PAYIN";
    private static final int STATUS_ENABLED = 1;

    private final ObjectMapper objectMapper;

    @Override
    public QueryWrapper<MerchantFeeRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String orderType = params.getStr("orderType");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String payChannel = params.getStr("payChannel");
        String feeMode = params.getStr("feeMode");
        String feeBearer = params.getStr("feeBearer");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.eq(StrUtil.isNotBlank(orderType), "order_type", normalize(orderType));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(payChannel), "pay_channel", normalize(payChannel));
        wrapper.eq(StrUtil.isNotBlank(feeMode), "fee_mode", normalize(feeMode));
        wrapper.eq(StrUtil.isNotBlank(feeBearer), "fee_bearer", normalize(feeBearer));
        return wrapper;
    }

    @Override
    public MerchantFeeResult calculatePayin(PayOrderEntity order) {
        MerchantFeeRuleEntity rule = selectPayinRule(order);
        MerchantFeeAmount amount;
        try {
            amount = MerchantFeeCalculator.calculate(order.getAmount(), rule);
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

    private MerchantFeeRuleEntity selectPayinRule(PayOrderEntity order) {
        Instant now = Instant.now();
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<MerchantFeeRuleEntity>()
                .eq("tenant_id", order.getTenantId())
                .eq("merchant_id", order.getMerchantId())
                .eq("order_type", ORDER_TYPE_PAYIN)
                .eq("currency", normalize(order.getCurrency()))
                .eq("status", STATUS_ENABLED)
                .and(w -> w.eq("merchant_app_id", order.getMerchantAppId()).or().isNull("merchant_app_id"))
                .and(w -> w.eq("country_code", normalize(order.getCountryCode())).or().isNull("country_code"))
                .and(w -> w.eq("pay_channel", normalize(order.getMethodCode())).or().isNull("pay_channel"))
                .and(w -> w.le("min_amount", order.getAmount()).or().isNull("min_amount"))
                .and(w -> w.ge("max_amount", order.getAmount()).or().isNull("max_amount"))
                .and(w -> w.le("effective_at", now).or().isNull("effective_at"))
                .and(w -> w.gt("expire_at", now).or().isNull("expire_at"));

        List<MerchantFeeRuleEntity> rules = baseDao.selectList(wrapper);
        return rules.stream()
                .max(Comparator
                        .comparingInt((MerchantFeeRuleEntity rule) -> matchScore(rule, order))
                        .thenComparing(rule -> -defaultPriority(rule.getPriority())))
                .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REQUEST, "Merchant fee rule is not configured"));
    }

    private int matchScore(MerchantFeeRuleEntity rule, PayOrderEntity order) {
        int score = 0;
        if (rule.getMerchantAppId() != null && rule.getMerchantAppId().equals(order.getMerchantAppId())) {
            score += 8;
        }
        if (StringUtils.equalsIgnoreCase(rule.getCountryCode(), order.getCountryCode())) {
            score += 4;
        }
        if (StringUtils.equalsIgnoreCase(rule.getPayChannel(), order.getMethodCode())) {
            score += 2;
        }
        if (rule.getMinAmount() != null || rule.getMaxAmount() != null) {
            score += 1;
        }
        return score;
    }

    private int defaultPriority(Integer priority) {
        return priority == null ? 100 : priority;
    }

    private String toSnapshotJson(MerchantFeeRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("orderType", rule.getOrderType());
        snapshot.put("countryCode", rule.getCountryCode());
        snapshot.put("currency", rule.getCurrency());
        snapshot.put("payChannel", rule.getPayChannel());
        snapshot.put("feeMode", rule.getFeeMode());
        snapshot.put("feeRate", decimalText(rule.getFeeRate()));
        snapshot.put("feeFixed", decimalText(rule.getFeeFixed()));
        snapshot.put("minFee", decimalText(rule.getMinFee()));
        snapshot.put("maxFee", decimalText(rule.getMaxFee()));
        snapshot.put("feeBearer", rule.getFeeBearer());
        snapshot.put("settleMode", rule.getSettleMode());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new ApiException(ApiErrorCode.SYSTEM_ERROR);
        }
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
