package com.gk.ledger.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dto.MerchantBalanceDTO;

public interface MerchantBalanceQueryService {
    PageData<MerchantBalanceDTO> page(DynMap params);
}
