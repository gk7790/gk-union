package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.amount.AmountRangeUtils;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dao.PaymentRouteRuleDao;
import com.gk.payment.dto.PaymentRouteRuleDTO;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.entity.PaymentRouteRuleEntity;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.service.PaymentRouteRuleService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentRouteRuleServiceImpl extends CrudServiceImpl<PaymentRouteRuleDao, PaymentRouteRuleEntity, PaymentRouteRuleDTO> implements PaymentRouteRuleService {
    private final PaymentRouteGroupDao paymentRouteGroupDao;
    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public PageData<PaymentRouteRuleDTO> page(DynMap params) {
        normalizePageParams(params);
        long pageNo = Math.max(params.getLong(Constant.PAGE, 1L), 1L);
        long limit = Math.max(params.getLong(Constant.LIMIT, 10L), 1L);
        params.put("offset", (pageNo - 1) * limit);
        params.put("limitValue", limit);

        Long total = baseDao.countPageWithName(params);
        List<PaymentRouteRuleDTO> list = total == null || total == 0L
                ? List.of()
                : baseDao.selectPageWithName(params);
        return new PageData<>(list, total == null ? 0L : total);
    }

    @Override
    public QueryWrapper<PaymentRouteRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<PaymentRouteRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long groupId = params.getLong("groupId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String ruleName = params.getStr("ruleName");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(groupId != null, "group_id", groupId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(ruleName), "rule_name", ruleName);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        return wrapper;
    }

    private void normalizePageParams(DynMap params) {
        normalizeParam(params, "direction");
        normalizeParam(params, "countryCode");
        normalizeParam(params, "currency");
        normalizeParam(params, "methodCode");
    }

    private void normalizeParam(DynMap params, String key) {
        String value = params.getStr(key);
        if (StrUtil.isNotBlank(value)) {
            params.put(key, normalize(value));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PaymentRouteRuleDTO dto) {
        validateRuleBinding(dto);
        super.save(dto);
        evictPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentRouteRuleDTO dto) {
        validateRuleBinding(dto);
        super.update(dto);
        evictPlanCache();
    }

    @Override
    public void delete(Long[] ids) {
        super.delete(ids);
        evictPlanCache();
    }

    @Override
    public void delete(Long id) {
        super.delete(id);
        evictPlanCache();
    }

    private void validateRuleBinding(PaymentRouteRuleDTO dto) {
        if (dto == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route rule is required");
        }
        validateAmountRange(dto);
        PaymentRouteGroupEntity group = dto.getGroupId() == null ? null : paymentRouteGroupDao.selectById(dto.getGroupId());
        if (group == null) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Payment route group does not exist");
        }

        dto.setTenantId(group.getTenantId());
        dto.setDirection(normalize(dto.getDirection()));
        dto.setCountryCode(normalize(dto.getCountryCode()));
        dto.setCurrency(normalize(dto.getCurrency()));
        dto.setMethodCode(normalize(dto.getMethodCode()));

        if (notEqualsCode(dto.getDirection(), group.getDirection())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule direction must match route group");
        }
        if (notEqualsCode(dto.getCountryCode(), group.getCountryCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule country must match route group");
        }
        if (notEqualsCode(dto.getCurrency(), group.getCurrency())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule currency must match route group");
        }
        if (notEqualsCode(dto.getMethodCode(), group.getMethodCode())) {
            throw new GkException(ErrorCode.BAD_REQUEST, "Route rule method must match route group");
        }
    }

    private void validateAmountRange(PaymentRouteRuleDTO dto) {
        if (dto == null) {
            return;
        }
        try {
            AmountRangeUtils.validateConfigRange(dto.getMinAmount(), dto.getMaxAmount());
        } catch (IllegalArgumentException ex) {
            throw new GkException(ErrorCode.BAD_REQUEST, ex.getMessage());
        }
    }

    private String normalize(String value) {
        return StrUtil.nullToEmpty(value).trim().toUpperCase(Locale.ROOT);
    }

    private boolean notEqualsCode(String left, String right) {
        return !Strings.CI.equals(StringUtils.trim(left), StringUtils.trim(right));
    }

    private void evictPlanCache() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
