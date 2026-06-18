package com.gk.merchant.service;

import com.gk.merchant.dto.MerchantOptionsDTO;

public interface MerchantOptionsService {

    /**
     * 按当前登录主体返回商户/APP 下拉选项。
     */
    MerchantOptionsDTO options();
}
