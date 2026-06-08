package com.gk.psp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.psp.dao.PspRouteRuleDao;
import com.gk.psp.dto.PspRouteRuleDTO;
import com.gk.psp.entity.PspRouteRuleEntity;
import com.gk.psp.service.PspRouteRuleService;
import org.springframework.stereotype.Service;

@Service
public class PspRouteRuleServiceImpl extends CrudServiceImpl<PspRouteRuleDao, PspRouteRuleEntity, PspRouteRuleDTO> implements PspRouteRuleService {

    @Override
    public QueryWrapper<PspRouteRuleEntity> getWrapper(DynMap params) {
        QueryWrapper<PspRouteRuleEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long pspId = params.getLong("pspId", null);
        Long pspMethodId = params.getLong("pspMethodId", null);
        Long pspMerchantId = params.getLong("pspMerchantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String countryCode = params.getStr("countryCode");
        String currency = params.getStr("currency");
        String methodCode = params.getStr("methodCode");
        String direction = params.getStr("direction");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(pspId != null, "psp_id", pspId);
        wrapper.eq(pspMethodId != null, "psp_method_id", pspMethodId);
        wrapper.eq(pspMerchantId != null, "psp_merchant_id", pspMerchantId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(currency), "currency", currency);
        wrapper.eq(StrUtil.isNotBlank(methodCode), "method_code", methodCode);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        return wrapper;
    }
}
