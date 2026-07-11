package com.gk.ledger.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.ledger.dto.MerchantBalanceDTO;
import com.gk.ledger.dto.MerchantWalletBalanceDTO;

import java.util.List;

public interface MerchantBalanceQueryService {
    PageData<MerchantBalanceDTO> page(DynMap params);

    List<MerchantWalletBalanceDTO> listForCurrentMerchant(String currency);
}
