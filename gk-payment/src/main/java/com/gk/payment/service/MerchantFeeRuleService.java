package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.payment.dto.MerchantFeeRuleDTO;
import com.gk.payment.dto.MerchantFeeViewResponse;
import com.gk.payment.entity.MerchantFeeRuleEntity;
import com.gk.payment.entity.PayinOrderEntity;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.fee.MerchantFeeResult;

public interface MerchantFeeRuleService extends CrudService<MerchantFeeRuleEntity, MerchantFeeRuleDTO> {
    MerchantFeeViewResponse merchantView(DynMap params);

    MerchantFeeResult calculatePayin(PayinOrderEntity order);

    @SuppressWarnings("unused")
    MerchantFeeResult calculatePayout(PayoutOrderEntity order);
}
