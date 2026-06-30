package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.DynMap;
import com.gk.infra.enums.StatusEnum;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.dto.PspProviderDTO;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.service.PspProviderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class PspProviderServiceImpl extends CrudServiceImpl<PspProviderDao, PspProviderEntity, PspProviderDTO> implements PspProviderService {
    private static final String EMPTY_CONFIG_JSON = "{}";

    @Override
    public QueryWrapper<PspProviderEntity> getWrapper(DynMap params) {
        QueryWrapper<PspProviderEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        java.util.List<Integer> statusList = StatusEnum.normalizeQueryStatus(params.getList("status", Integer.class, StatusEnum.defaultStatus()));
        String pspCode = params.getStr("pspCode");
        String pspName = params.getStr("pspName");
        String countryCode = params.getStr("countryCode");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.in("status", statusList);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.like(StrUtil.isNotBlank(pspName), "psp_name", pspName);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        return wrapper;
    }

    @Override
    public void save(PspProviderDTO dto) {
        fillTenant(dto, true);
        normalizeConfigJson(dto, true);
        super.save(dto);
        evictPayinPlanCache();
    }

    @Override
    public void update(PspProviderDTO dto) {
        fillTenant(dto, false);
        normalizeConfigJson(dto, false);
        super.update(dto);
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long[] ids) {
        baseDao.update(null, new UpdateWrapper<PspProviderEntity>()
                .set("status", StatusEnum.STOP.code())
                .in("id", java.util.Arrays.asList(ids)));
        evictPayinPlanCache();
    }

    @Override
    public void delete(Long id) {
        baseDao.update(null, new UpdateWrapper<PspProviderEntity>()
                .set("status", StatusEnum.STOP.code())
                .eq("id", id));
        evictPayinPlanCache();
    }

    private void evictPayinPlanCache() {
        // PSP module does not depend on payment; cache eviction is handled by integration wiring.
    }

    private void fillTenant(PspProviderDTO dto, boolean required) {
        if (dto == null) {
            return;
        }
        if (dto.getTenantId() != null) {
            return;
        }
        Long currentTenantId = ReqContextHolder.getTenantId();
        if (currentTenantId != null) {
            dto.setTenantId(currentTenantId);
            return;
        }
        if (required) {
            throw new GkException(ErrorCode.BAD_REQUEST, "tenantId is required");
        }
    }

    private void normalizeConfigJson(PspProviderDTO dto, boolean defaultWhenMissing) {
        if (dto == null) {
            return;
        }
        if (dto.getConfigJson() == null && !defaultWhenMissing) {
            return;
        }
        String configJson = StringUtils.trimToNull(dto.getConfigJson());
        if (configJson == null) {
            dto.setConfigJson(EMPTY_CONFIG_JSON);
            return;
        }
        try {
            JSON.parse(configJson);
            dto.setConfigJson(configJson);
        } catch (JSONException ex) {
            throw new GkException(ErrorCode.JSON_FORMAT_ERROR, ex, "configJson");
        }
    }
}
