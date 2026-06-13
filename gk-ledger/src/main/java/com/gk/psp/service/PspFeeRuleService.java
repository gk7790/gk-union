package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.psp.dto.PspFeeRuleDTO;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.fee.PspFeeResult;

public interface PspFeeRuleService extends CrudService<PspFeeRuleEntity, PspFeeRuleDTO> {
    PspFeeResult calculatePayin(PayOrderEntity order);

    PspFeeResult calculatePayout(PayoutOrderEntity order);
}
