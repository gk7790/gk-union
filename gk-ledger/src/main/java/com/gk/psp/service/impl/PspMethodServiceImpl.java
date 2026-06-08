package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspMethodDao;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.entity.PspMethodEntity;
import com.gk.psp.service.PspMethodService;
import org.springframework.stereotype.Service;

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
}
