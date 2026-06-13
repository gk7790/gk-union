package com.gk.ledger.adjust.service;

import com.gk.common.core.service.CrudService;
import com.gk.ledger.adjust.dto.MerchantBalanceAdjustOrderDTO;
import com.gk.ledger.adjust.entity.MerchantBalanceAdjustOrderEntity;

public interface MerchantBalanceAdjustOrderService extends CrudService<MerchantBalanceAdjustOrderEntity, MerchantBalanceAdjustOrderDTO> {
    MerchantBalanceAdjustOrderDTO submit(MerchantBalanceAdjustOrderDTO dto);
}
