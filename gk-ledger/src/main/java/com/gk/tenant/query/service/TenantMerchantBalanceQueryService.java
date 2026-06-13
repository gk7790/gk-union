package com.gk.tenant.query.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.tenant.query.dto.TenantMerchantBalanceDTO;

public interface TenantMerchantBalanceQueryService {
    PageData<TenantMerchantBalanceDTO> page(DynMap params);
}
