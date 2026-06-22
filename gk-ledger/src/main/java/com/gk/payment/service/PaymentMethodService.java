package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.payment.dto.PaymentMethodDTO;
import com.gk.payment.entity.PaymentMethodEntity;

import java.util.List;

public interface PaymentMethodService extends CrudService<PaymentMethodEntity, PaymentMethodDTO> {
    List<PaymentMethodDTO> getDict(DynMap params);

    List<LabelDTO> getLabelDict(DynMap params);
}
