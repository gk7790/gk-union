package com.gk.ledger.query.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.query.dto.TenantMerchantBalanceDTO;

public interface TenantMerchantBalanceQueryService {
    PageData<TenantMerchantBalanceDTO> page(DynMap params);
}
