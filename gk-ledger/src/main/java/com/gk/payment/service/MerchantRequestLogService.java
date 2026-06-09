package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.MerchantRequestLogDTO;
import com.gk.payment.entity.MerchantRequestLogEntity;

public interface MerchantRequestLogService extends CrudService<MerchantRequestLogEntity, MerchantRequestLogDTO> {
    void record(MerchantRequestLogEntity entity);
}
