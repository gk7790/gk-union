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
import com.gk.payment.entity.PayoutOrderEntity;
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
    private static final String ORDER_TYPE_PAYOUT = "PAYOUT";
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
        return calculate(
                order.getTenantId(),
                order.getMerchantId(),
                order.getMerchantAppId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                ORDER_TYPE_PAYIN
        );
    }

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
                ORDER_TYPE_PAYOUT
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
            String orderType
    ) {
        MerchantFeeRuleEntity rule = selectRule(tenantId, merchantId, merchantAppId, countryCode, currency, methodCode, orderAmount, orderType);
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

    private MerchantFeeRuleEntity selectRule(
            Long tenantId,
            Long merchantId,
            Long merchantAppId,
            String countryCode,
            String currency,
            String methodCode,
            BigDecimal orderAmount,
            String orderType
    ) {
        Instant now = Instant.now();
        QueryWrapper<MerchantFeeRuleEntity> wrapper = new QueryWrapper<MerchantFeeRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("merchant_id", merchantId)
                .eq("order_type", orderType)
                .eq("currency", normalize(currency))
                .eq("status", STATUS_ENABLED)
                .and(w -> w.eq("merchant_app_id", merchantAppId).or().isNull("merchant_app_id"))
                .and(w -> w.eq("country_code", normalize(countryCode)).or().isNull("country_code"))
                .and(w -> w.eq("pay_channel", normalize(methodCode)).or().isNull("pay_channel"))
                .and(w -> w.le("min_amount", orderAmount).or().isNull("min_amount"))
                .and(w -> w.ge("max_amount", orderAmount).or().isNull("max_amount"))
                .and(w -> w.le("effective_at", now).or().isNull("effective_at"))
                .and(w -> w.gt("expire_at", now).or().isNull("expire_at"));

        List<MerchantFeeRuleEntity> rules = baseDao.selectList(wrapper);
        return rules.stream()
                .max(Comparator
                        .comparingInt((MerchantFeeRuleEntity rule) -> matchScore(rule, merchantAppId, countryCode, methodCode))
                        .thenComparing(rule -> -defaultPriority(rule.getPriority())))
                .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REQUEST, "Merchant fee rule is not configured"));
    }

    private int matchScore(MerchantFeeRuleEntity rule, Long merchantAppId, String countryCode, String methodCode) {
        int score = 0;
        if (rule.getMerchantAppId() != null && rule.getMerchantAppId().equals(merchantAppId)) {
            score += 8;
        }
        if (StringUtils.equalsIgnoreCase(rule.getCountryCode(), countryCode)) {
            score += 4;
        }
        if (StringUtils.equalsIgnoreCase(rule.getPayChannel(), methodCode)) {
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
