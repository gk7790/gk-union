package com.gk.merchant.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.entity.MerchantAppEntity;

import java.util.List;

public interface MerchantAppService extends CrudService<MerchantAppEntity, MerchantAppDTO> {

    /**
     * 重置 API 密钥，返回含明文 apiSecret �?DTO（仅此一次可见）�?     */
    MerchantAppDTO resetApiSecret(Long id);

    /**
     * 从测�?APP 创建正式 APP�?     */
    MerchantAppDTO createProductionApp(Long testAppId);

    List<MerchantAppDTO> getDict(DynMap params);
}
