package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.enums.PayDirectionEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspAccountDao;
import com.gk.psp.dao.PspFeeRuleDao;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dto.PspFeeRuleDTO;
import com.gk.psp.entity.PspAccountEntity;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.fee.PspFeeCalculator;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.request.PspOrderRequest;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PspFeeRuleServiceImpl extends CrudServiceImpl<PspFeeRuleDao, PspFeeRuleEntity, PspFeeRuleDTO> implements PspFeeRuleService {
    @Autowired
    private PspMethodDao pspMethodDao;
    @Autowired
    private PspAccountDao pspAccountDao;

    @Override
    public QueryWrapper<PspFeeRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<PspFeeRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String pspMethodCode = params.getStr("pspMethodCode");
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
        wrapper.eq(StrUtil.isNotBlank(pspMethodCode), "psp_method_code", pspMethodCode);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(feeMode), "fee_mode", normalize(feeMode));
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PspFeeRuleDTO dto) {
        fillPspMethodSnapshot(dto);
        inheritTenantFromPspAccount(dto);
        PspFeeRuleEntity entity = ConvertUtils.sourceToTarget(dto, PspFeeRuleEntity.class);
        insert(entity);
        evictPayinPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspFeeRuleDTO dto) {
        fillPspMethodSnapshot(dto);
        inheritTenantFromPspAccount(dto);
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
    public PspFeeResult calculatePayin(PspOrderRequest order) {
        return calculate(
                order.getTenantId(),
                order.getPspId(),
                order.getPspAccountId(),
                order.getPspMethodId(),
                null,
                order.getCurrency(),
                order.getMethodCode(),
                order.getAmount(),
                PayDirectionEnum.PAYIN.code()
        );
    }

    @Override
    public PspFeeResult calculatePayout(PspOrderRequest order) {
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
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
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
        PspFeeRuleEntity rule = baseDao.selectBestMatchForOrder(
                tenantId,
                pspId,
                pspAccountId,
                pspMethodId,
                normalize(countryCode),
                normalize(currency),
                normalize(methodCode),
                orderAmount,
                direction,
                now,
                StatusEnum.NORMAL.code()
        );
        if (rule == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        return rule;
    }

    private String toSnapshotJson(PspFeeRuleEntity rule) {
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

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private void fillPspMethodSnapshot(PspFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        if (dto.getPspMethodId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        PspMethodEntity method = pspMethodDao.selectById(dto.getPspMethodId());
        if (method == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        // Keep PSP fee rule snapshots consistent with the selected PSP Method.
        dto.setPspId(method.getPspId());
        dto.setMethodCode(normalize(method.getMethodCode()));
        dto.setPspMethodCode(StringUtils.trimToNull(method.getPspMethodCode()));
    }

    private void inheritTenantFromPspAccount(PspFeeRuleDTO dto) {
        if (dto == null) {
            return;
        }
        if (dto.getPspAccountId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }

        PspAccountEntity account = pspAccountDao.selectById(dto.getPspAccountId());
        if (account == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        if (account.getTenantId() == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }
        if (dto.getPspId() != null && !dto.getPspId().equals(account.getPspId())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "PSP payment method not found");
        }

        // Cost fee rule tenant follows the selected PSP account; client tenantId is ignored on save/update.
        dto.setPspId(account.getPspId());
        dto.setTenantId(account.getTenantId());
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPayinPlanCache() {
        // PSP ģ鲻ֱ payment 棬ģ¼ͳһ
    }
}
