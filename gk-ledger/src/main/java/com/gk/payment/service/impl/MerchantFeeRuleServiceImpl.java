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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantFeeRuleServiceImpl extends CrudServiceImpl<MerchantFeeRuleDao, MerchantFeeRuleEntity, MerchantFeeRuleDTO> implements MerchantFeeRuleService {
    private static final String SETTLE_MODE_DEDUCT = "DEDUCT";
    private static final String SETTLE_MODE_ADD = "ADD";

    @Override
    public void save(MerchantFeeRuleDTO dto) {
        normalizePersistFields(dto);
        super.save(dto);
    }

    @Override
    public void update(MerchantFeeRuleDTO dto) {
        normalizePersistFields(dto);
        super.update(dto);
    }

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
            String direction
    ) {
        // 先按商户、币种、方向、支付方式、金额区间等条件选中一条有效规则，再交给纯计算器算金额。
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
     * 生成订单费率快照。
     * <p>
     * 快照只保留计费和审计需要的字段，不保存整条规则对象，避免后续规则字段变化影响历史订单解析。
     */
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
     * 使用普通十进制字符串保存金额/费率，避免 BigDecimal 科学计数法进入 JSON 快照。
     */
    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private void normalizePersistFields(MerchantFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setFeeBearer(null);
        String direction = StringUtils.defaultIfBlank(dto.getDirection(), currentDirection(dto.getId()));
        dto.setSettleMode(PayDirectionEnum.PAYOUT.matches(direction) ? SETTLE_MODE_ADD : SETTLE_MODE_DEDUCT);
    }

    private String currentDirection(Long id) {
        if (id == null) {
            return null;
        }
        MerchantFeeRuleEntity entity = baseDao.selectById(id);
        return entity == null ? null : entity.getDirection();
    }

    /**
     * 统一将配置编码和请求编码转换成大写后比较，降低前端输入大小写差异带来的匹配失败。
     */
    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
