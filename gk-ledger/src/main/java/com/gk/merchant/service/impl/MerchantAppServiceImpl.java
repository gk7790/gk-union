package com.gk.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.merchant.service.MerchantAppService;
import org.springframework.stereotype.Service;

@Service
public class MerchantAppServiceImpl extends CrudServiceImpl<MerchantAppDao, MerchantAppEntity, MerchantAppDTO> implements MerchantAppService {

    @Override
    public QueryWrapper<MerchantAppEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantAppEntity> wrapper = new QueryWrapper<>();

        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Integer status = params.containsKey("status") ? params.getInt("status") : null;
        String appId = params.getStr("appId");
        String appName = params.getStr("appName");
        String appType = params.getStr("appType");
        String signType = params.getStr("signType");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(status != null, "status", status);
        wrapper.eq(StrUtil.isNotBlank(appId), "app_id", appId);
        wrapper.like(StrUtil.isNotBlank(appName), "app_name", appName);
        wrapper.eq(StrUtil.isNotBlank(appType), "app_type", appType);
        wrapper.eq(StrUtil.isNotBlank(signType), "sign_type", signType);

        return wrapper;
    }
}
