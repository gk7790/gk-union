package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.service.PspMethodService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PspMethodServiceImpl extends CrudServiceImpl<PspMethodDao, PspMethodEntity, PspMethodDTO> implements PspMethodService {

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
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");

        wrapper.select("method_code", "MIN(method_name) AS method_name");
        wrapper.eq("status", StatusEnum.NORMAL.code());
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", normalize(countryCode));
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", normalize(currency));
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", normalize(direction));
        wrapper.groupBy("method_code");
        wrapper.orderByAsc("method_code");

        return baseDao.selectList(wrapper).stream()
                .map(item -> new LabelDTO(item.getMethodCode(), StringUtils.defaultIfBlank(item.getMethodName(), item.getMethodCode())))
                .toList();
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
