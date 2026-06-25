package com.gk.psp.service;

import com.gk.common.core.service.CrudService;
import com.gk.psp.dto.PspFeeRuleDTO;
import com.gk.psp.entity.PspFeeRuleEntity;
import com.gk.psp.fee.PspFeeResult;
import com.gk.psp.request.PspOrderRequest;

public interface PspFeeRuleService extends CrudService<PspFeeRuleEntity, PspFeeRuleDTO> {
    PspFeeResult calculatePayin(PspOrderRequest order);

    PspFeeResult calculatePayout(PspOrderRequest order);
}
