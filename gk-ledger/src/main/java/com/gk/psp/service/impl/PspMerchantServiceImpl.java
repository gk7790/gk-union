package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspMerchantDao;
import com.gk.psp.dto.PspMerchantDTO;
import com.gk.psp.entity.PspMerchantEntity;
import com.gk.psp.service.PspMerchantService;
import org.springframework.stereotype.Service;

@Service
public class PspMerchantServiceImpl extends CrudServiceImpl<PspMerchantDao, PspMerchantEntity, PspMerchantDTO> implements PspMerchantService {

    @Override
    public QueryWrapper<PspMerchantEntity> getWrapper(DynMap params) {
        QueryWrapper<PspMerchantEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantScopeId = params.getLong("merchantScopeId", null);
        Long pspId = params.getLong("pspId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String pspCode = params.getStr("pspCode");
        String pspMerchantNo = params.getStr("pspMerchantNo");
        String pspMerchantName = params.getStr("pspMerchantName");
        String secretType = params.getStr("secretType");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantScopeId != null, "merchant_scope_id", merchantScopeId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(pspCode), "psp_code", pspCode);
        wrapper.eq(StrUtil.isNotBlank(pspMerchantNo), "psp_merchant_no", pspMerchantNo);
        wrapper.like(StrUtil.isNotBlank(pspMerchantName), "psp_merchant_name", pspMerchantName);
        wrapper.eq(StrUtil.isNotBlank(secretType), "secret_type", secretType);
        return wrapper;
    }
}
