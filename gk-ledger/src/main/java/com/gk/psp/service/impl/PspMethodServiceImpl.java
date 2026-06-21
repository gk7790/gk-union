package com.gk.psp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.DynMap;
import com.gk.payment.plan.PayinPlanCache;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.service.PspMethodService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class PspMethodServiceImpl extends CrudServiceImpl<PspMethodDao, PspMethodEntity, PspMethodDTO> implements PspMethodService {
    private static final String EMPTY_CONFIG_JSON = "{}";

    @Autowired
    private PayinPlanCache payinPlanCache;

    @Override
    public QueryWrapper<PspMethodEntity> getWrapper(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspCode = params.getStr("pspCode");
        String methodCode = params.getStr("methodCode");
        String pspMethodCode = params.getStr("pspMethodCode");
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");
        if (StrUtil.isBlank(direction)) {
            direction = params.getStr("orderType");
        }

        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(pspMethodCode), "psp_method_code", pspMethodCode);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        return wrapper;
    }

    @Override
    public List<LabelDTO> getMethodCodeDict(DynMap params) {
        QueryWrapper<PspMethodEntity> wrapper = new QueryWrapper<>();
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");

        wrapper.select("method_code", "MIN(method_name) AS method_name");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.groupBy("method_code");
        wrapper.orderByAsc("method_code");

        return baseDao.selectList(wrapper).stream()
                .map(item -> new LabelDTO(item.getMethodCode(), StringUtils.defaultIfBlank(item.getMethodName(), item.getMethodCode())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PspMethodDTO dto) {
        normalizeConfigJson(dto);
        super.save(dto);
        evictPayinPlanCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PspMethodDTO dto) {
        normalizeConfigJson(dto);
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

    private void normalizeConfigJson(PspMethodDTO dto) {
        if (dto == null) {
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

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private void evictPayinPlanCache() {
        // PSP Method 配置会影响路由和上游提交参数，变更后必须清空 PayinPlan 缓存。
        if (payinPlanCache != null) {
            payinPlanCache.evictAll();
        }
    }
}
