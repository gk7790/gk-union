package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.MerchantFeeRuleDTO;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;

public interface MerchantFeeRuleService extends CrudService<MerchantFeeRuleEntity, MerchantFeeRuleDTO> {
    MerchantFeeResult calculatePayin(PayOrderEntity order);

    MerchantFeeResult calculatePayout(PayoutOrderEntity order);
}
