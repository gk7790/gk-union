package com.gk.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.merchant.dao.MerchantDao;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.merchant.service.MerchantService;
import org.springframework.stereotype.Service;

@Service
public class MerchantServiceImpl extends CrudServiceImpl<MerchantDao, MerchantEntity, MerchantDTO> implements MerchantService {

    @Override
    public QueryWrapper<MerchantEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantEntity> wrapper = new QueryWrapper<>();

        Long tenantId = params.getLong("tenantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String merchantNo = params.getStr("merchantNo");
        String merchantName = params.getStr("merchantName");
        String countryCode = params.getStr("countryCode");
        String defaultCurrency = params.getStr("defaultCurrency");
        String riskStatus = params.getStr("riskStatus");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(merchantNo), "merchant_no", merchantNo);
        wrapper.like(StrUtil.isNotBlank(merchantName), "merchant_name", merchantName);
        wrapper.eq(StrUtil.isNotBlank(countryCode), "country_code", countryCode);
        wrapper.eq(StrUtil.isNotBlank(defaultCurrency), "default_currency", defaultCurrency);
        wrapper.eq(StrUtil.isNotBlank(riskStatus), "risk_status", riskStatus);

        return wrapper;
    }
}
