package com.gk.merchant.service;

import com.gk.common.core.service.CrudService;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.entity.MerchantAppEntity;

public interface MerchantAppService extends CrudService<MerchantAppEntity, MerchantAppDTO> {

    /**
     * 重置 API 密钥，返回含明文 apiSecret 的 DTO（仅此一次可见）。
     */
    MerchantAppDTO resetApiSecret(Long id);
}
