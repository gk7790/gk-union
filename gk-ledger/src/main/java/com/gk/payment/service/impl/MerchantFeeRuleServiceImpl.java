package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
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

    /**
     * 后台管理页的查询条件组装。
     * <p>
     * 这里只负责列表/分页筛选，不参与下单时的费率命中；下单命中规则见 {@link #selectRule}。
     */
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
        String methodCode = params.getStr("methodCode");
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
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(feeMode), "fee_mode", normalize(feeMode));
        wrapper.eq(StrUtil.isNotBlank(feeBearer), "fee_bearer", normalize(feeBearer));
        return wrapper;
    }

    /**
     * 计算代收订单的商户手续费。
     * <p>
     * 代收成功后入账使用 settleAmount，当前由 {@link MerchantFeeCalculator}
     * 根据手续费承担方计算“订单金额 - 商户手续费”或保持订单金额不变。
     */
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
                PayDirectionEnum.PAYIN.code()
        );
    }

    /**
     * 计算代付订单的商户手续费。
     * <p>
     * 代付侧调用方会将返回的 merchantFeeAmount 加到 totalDebitAmount，
     * 冻结和扣减时按“代付本金 + 商户手续费”处理。
     */
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
            String orderType
    ) {
        // 先按商户、币种、方向、支付方式、金额区间等条件选中一条有效规则，再交给纯计算器算金额。
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
        // 下单时保存费率快照，后续即使规则被修改，历史订单仍能还原当时的计费口径。
        result.setSnapshotJson(toSnapshotJson(rule));
        return result;
    }

    /**
     * 选择下单时真正生效的商户费率规则。
     * <p>
     * 必须匹配租户、商户、订单方向、币种和启用状态；应用、国家、支付方式、金额区间、生效时间支持空值兜底。
     * 多条规则同时命中时，优先选择更精确的规则，再按 priority 数值越小越优先。
     */
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
                .eq("status", StatusEnum.NORMAL.code())
                // merchant_app_id / country_code / method_code 为空表示通用规则；不为空表示更精确的专属规则。
                .and(w -> w.eq("merchant_app_id", merchantAppId).or().isNull("merchant_app_id"))
                .and(w -> w.eq("country_code", normalize(countryCode)).or().isNull("country_code"))
                .and(w -> w.eq("method_code", normalize(methodCode)).or().isNull("method_code"))
                // 金额区间为空表示不限制；有值时订单金额必须落在区间内。
                .and(w -> w.le("min_amount", orderAmount).or().isNull("min_amount"))
                .and(w -> w.ge("max_amount", orderAmount).or().isNull("max_amount"))
                // 生效时间为空表示立即生效，失效时间为空表示长期有效。
                .and(w -> w.le("effective_at", now).or().isNull("effective_at"))
                .and(w -> w.gt("expire_at", now).or().isNull("expire_at"));

        List<MerchantFeeRuleEntity> rules = baseDao.selectList(wrapper);
        return rules.stream()
                // matchScore 越高表示规则越精确；priority 数字越小越优先，因此这里取负数参与 max 比较。
                .max(Comparator
                        .comparingInt((MerchantFeeRuleEntity rule) -> matchScore(rule, merchantAppId, countryCode, methodCode))
                        .thenComparing(rule -> -defaultPriority(rule.getPriority())))
                .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REQUEST, "Merchant fee rule is not configured"));
    }

    /**
     * 计算规则精确度分数。
     * <p>
     * 应用专属权重最高，其次是国家、支付方式、金额区间；分数越高越优先。
     */
    private int matchScore(MerchantFeeRuleEntity rule, Long merchantAppId, String countryCode, String methodCode) {
        int score = 0;
        if (rule.getMerchantAppId() != null && rule.getMerchantAppId().equals(merchantAppId)) {
            score += 8;
        }
        if (StringUtils.equalsIgnoreCase(rule.getCountryCode(), countryCode)) {
            score += 4;
        }
        if (StringUtils.equalsIgnoreCase(rule.getMethodCode(), methodCode)) {
            score += 2;
        }
        if (rule.getMinAmount() != null || rule.getMaxAmount() != null) {
            score += 1;
        }
        return score;
    }

    /**
     * 未配置 priority 时按 100 处理，与表默认值保持一致。
     */
    private int defaultPriority(Integer priority) {
        return priority == null ? 100 : priority;
    }

    /**
     * 生成订单费率快照。
     * <p>
     * 快照只保留计费和审计需要的字段，不保存整条规则对象，避免后续规则字段变化影响历史订单解析。
     */
    private String toSnapshotJson(MerchantFeeRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("orderType", rule.getOrderType());
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
     * 使用普通十进制字符串保存金额/费率，避免 BigDecimal 科学计数法进入 JSON 快照。
     */
    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    /**
     * 统一将配置编码和请求编码转换成大写后比较，降低前端输入大小写差异带来的匹配失败。
     */
    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
