package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.PaymentRouteGroupDao;
import com.gk.payment.dto.PaymentRouteGroupDTO;
import com.gk.payment.entity.PaymentRouteGroupEntity;
import com.gk.payment.plan.PaymentPlanCacheService;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.payment.service.PaymentRouteGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentRouteGroupServiceImpl extends CrudServiceImpl<PaymentRouteGroupDao, PaymentRouteGroupEntity, PaymentRouteGroupDTO> implements PaymentRouteGroupService {
    private final PayinPlanCache payinPlanCache;
    private final PaymentPlanCacheService paymentPlanCacheService;

    @Override
    public QueryWrapper<PaymentRouteGroupEntity> getWrapper(DynMap params) {
        QueryWrapper<PaymentRouteGroupEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String groupCode = params.getStr("groupCode");
        String groupName = params.getStr("groupName");
        String direction = params.getStr("direction");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String strategy = params.getStr("strategy");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(status != null, "status", status);
        wrapper.like(StrUtil.isNotBlank(groupCode), "group_code", groupCode);
        wrapper.like(StrUtil.isNotBlank(groupName), "group_name", groupName);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", normalize(methodCode));
        wrapper.eq(StrUtil.isNotBlank(strategy), "strategy", normalize(strategy));
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PaymentRouteGroupDTO dto) {
        normalize(dto);
        super.save(dto);
        evictPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PaymentRouteGroupDTO dto) {
        normalize(dto);
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

    private void normalize(PaymentRouteGroupDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setGroupCode(normalize(dto.getGroupCode()));
        dto.setDirection(normalize(dto.getDirection()));
        dto.setCountryCode(normalize(dto.getCountryCode()));
        dto.setCurrency(normalize(dto.getCurrency()));
        dto.setMethodCode(normalize(dto.getMethodCode()));
        dto.setStrategy(StrUtil.blankToDefault(normalize(dto.getStrategy()), "PRIORITY_WEIGHT"));
    }

    private String normalize(String value) {
        return StrUtil.nullToEmpty(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPlanCache() {
        payinPlanCache.evictAll();
        paymentPlanCacheService.evictAll();
    }
}
