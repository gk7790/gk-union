package com.gk.adjustment.service;

import com.gk.common.core.service.CrudService;
import com.gk.adjustment.dto.MerchantBalanceAdjustOrderDTO;
import com.gk.adjustment.entity.MerchantBalanceAdjustOrderEntity;

public interface MerchantBalanceAdjustOrderService extends CrudService<MerchantBalanceAdjustOrderEntity, MerchantBalanceAdjustOrderDTO> {
    MerchantBalanceAdjustOrderDTO submit(MerchantBalanceAdjustOrderDTO dto);
}
