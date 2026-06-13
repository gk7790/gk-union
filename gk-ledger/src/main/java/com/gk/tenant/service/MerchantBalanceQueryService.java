package com.gk.tenant.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.tenant.dto.MerchantBalanceDTO;

public interface MerchantBalanceQueryService {
    PageData<MerchantBalanceDTO> page(DynMap params);
}
