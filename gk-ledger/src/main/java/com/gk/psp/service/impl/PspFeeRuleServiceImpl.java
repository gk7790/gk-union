package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.NumberUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.openapi.error.ApiErrorCode;
import com.gk.openapi.error.ApiException;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dto.PspFeeRuleDTO;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.fee.PspFeeCalculator;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PspFeeRuleServiceImpl extends CrudServiceImpl<PspFeeRuleDao, PspFeeRuleEntity, PspFeeRuleDTO> implements PspFeeRuleService {
    @Autowired
    private PayinPlanCache payinPlanCache;

    @Override
    public QueryWrapper<PspFeeRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String feeMode = params.getStr("feeMode");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(feeMode), "fee_mode", normalize(feeMode));
        return wrapper;
    }

    @Override
    public void save(PspFeeRuleDTO dto) {
        PspFeeRuleEntity entity = ConvertUtils.sourceToTarget(dto, PspFeeRuleEntity.class);
        ReqContext context = ReqContextHolder.get();
        entity.setTenantId(context.getTenantId());
        if (SubjectTypeEnum.PLATFORM.code().equals(context.getSubjectType())) {
            if (!NumberUtils.isPositive(entity.getTenantId())) {
                entity.setTenantId(context.getTenantId());
            }
        }
        insert(entity);
        evictPayinPlanCache();
    }

    @Override
    public void update(PspFeeRuleDTO dto) {
        super.update(dto);
        evictPayinPlanCache();
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

    @Override
    public PspFeeResult calculatePayin(PayOrderEntity order) {
        return calculate(
                order.getTenantId(),
                order.getPspId(),
                order.getPspAccountId(),
                order.getPspMethodId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    @Override
    public PspFeeResult calculatePayout(PayoutOrderEntity order) {
        return calculate(
                order.getTenantId(),
                order.getPspId(),
                order.getPspAccountId(),
                order.getPspMethodId(),
                order.getCountryCode(),
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYOUT.code()
        );
    }

    private PspFeeResult calculate(
            Long tenantId,
            Long pspId,
            Long pspAccountId,
            Long pspMethodId,
            String countryCode,
            String currency,
            String methodCode,
            BigDecimal orderAmount,
            String direction
    ) {
        PspFeeRuleEntity rule = selectRule(tenantId, pspId, pspAccountId, pspMethodId, countryCode, currency, methodCode, orderAmount, direction);
        BigDecimal feeAmount;
        try {
            feeAmount = PspFeeCalculator.calculate(orderAmount, rule);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, ex.getMessage());
        }

        PspFeeResult result = new PspFeeResult();
        result.setRule(rule);
        result.setPspFeeAmount(feeAmount);
        result.setSnapshotJson(toSnapshotJson(rule));
        return result;
    }

    private PspFeeRuleEntity selectRule(
            Long tenantId,
            Long pspId,
            Long pspAccountId,
            Long pspMethodId,
            String countryCode,
            String currency,
            String methodCode,
            BigDecimal orderAmount,
            String direction
    ) {
        Instant now = Instant.now();
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<PspFeeRuleEntity>()
                .eq("tenant_id", tenantId)
                .eq("psp_id", pspId)
                .eq("direction", direction)
                .eq("currency", normalize(currency))
                .eq("status", StatusEnum.NORMAL.code())
                .and(w -> w.eq("psp_account_id", pspAccountId).or().isNull("psp_account_id"))
                .and(w -> w.eq("psp_method_id", pspMethodId).or().isNull("psp_method_id"))
                .and(w -> w.eq("country_code", normalize(countryCode)).or().isNull("country_code"))
                .and(w -> w.eq("method_code", normalize(methodCode)).or().isNull("method_code"))
                .and(w -> w.le("min_amount", orderAmount).or().isNull("min_amount"))
                .and(w -> w.ge("max_amount", orderAmount).or().isNull("max_amount"))
                .and(w -> w.le("effective_at", now).or().isNull("effective_at"))
                .and(w -> w.gt("expire_at", now).or().isNull("expire_at"));

        List<PspFeeRuleEntity> rules = baseDao.selectList(wrapper);
        return rules.stream()
                .max(Comparator
                        .comparingInt((PspFeeRuleEntity rule) -> matchScore(rule, pspAccountId, pspMethodId, countryCode, methodCode))
                        .thenComparing(rule -> -defaultPriority(rule.getPriority())))
                .orElseThrow(() -> new ApiException(ApiErrorCode.INVALID_REQUEST, "PSP fee rule is not configured"));
    }

    private int matchScore(PspFeeRuleEntity rule, Long pspAccountId, Long pspMethodId, String countryCode, String methodCode) {
        int score = 0;
        if (rule.getPspAccountId() != null && rule.getPspAccountId().equals(pspAccountId)) {
            score += 16;
        }
        if (rule.getPspMethodId() != null && rule.getPspMethodId().equals(pspMethodId)) {
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

    private int defaultPriority(Integer priority) {
        return priority == null ? 100 : priority;
    }

    private String toSnapshotJson(PspFeeRuleEntity rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleId", rule.getId());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("pspId", rule.getPspId());
        snapshot.put("pspAccountId", rule.getPspAccountId());
        snapshot.put("pspMethodId", rule.getPspMethodId());
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

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPayinPlanCache() {
        // PSP 成本费率会影响 PayinPlan 的成本核算字段，变更后必须清空缓存。
        if (payinPlanCache != null) {
            payinPlanCache.evictAll();
        }
    }
}
