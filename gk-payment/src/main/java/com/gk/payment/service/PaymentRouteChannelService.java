package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.model.DynMap;
import com.gk.payment.dto.PaymentRouteChannelDTO;
import com.gk.payment.dto.PaymentRouteChannelOptionsResponse;
import com.gk.payment.entity.PaymentRouteChannelEntity;

public interface PaymentRouteChannelService extends CrudService<PaymentRouteChannelEntity, PaymentRouteChannelDTO> {
    PaymentRouteChannelOptionsResponse options(DynMap params);
}
