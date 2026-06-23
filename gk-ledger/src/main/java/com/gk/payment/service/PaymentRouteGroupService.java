package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.PaymentRouteGroupCheckResponse;
import com.gk.payment.dto.PaymentRouteGroupDTO;
import com.gk.payment.entity.PaymentRouteGroupEntity;

public interface PaymentRouteGroupService extends CrudService<PaymentRouteGroupEntity, PaymentRouteGroupDTO> {
    PaymentRouteGroupCheckResponse check(Long id);
}
