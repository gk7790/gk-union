package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspBankMappingDao;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.entity.PspBankMappingEntity;
import com.gk.psp.service.PspBankMappingService;
import org.springframework.stereotype.Service;

@Service
public class PspBankMappingServiceImpl extends CrudServiceImpl<PspBankMappingDao, PspBankMappingEntity, PspBankMappingDTO>
        implements PspBankMappingService {

    @Override
    public QueryWrapper<PspBankMappingEntity> getWrapper(DynMap params) {
        QueryWrapper<PspBankMappingEntity> wrapper = new QueryWrapper<>();
        Long pspId = params.getLong("pspId", null);
        Long pspAccountId = params.getLong("pspAccountId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Long bankId = params.getLong("bankId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String direction = params.getStr("direction");
        String pspBankCode = params.getStr("pspBankCode");

        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspAccountId != null, "psp_account_id", pspAccountId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(bankId != null, "bank_id", bankId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        wrapper.eq(StrUtil.isNotBlank(pspBankCode), "psp_bank_code", pspBankCode);
        wrapper.orderByDesc("id");
        return wrapper;
    }
}
