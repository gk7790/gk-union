package com.gk.payment.service;

import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.payment.dto.PaymentPlanDetailResponse;
import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPreviewResponse;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.dto.PaymentPlanPublishResponse;
import com.gk.payment.dto.PaymentPlanVersionDTO;

public interface PaymentPlanAdminService {
    PageData<PaymentPlanVersionDTO> page(DynMap params);

    PaymentPlanDetailResponse detail(Long catalogId);

    PaymentPlanPreviewResponse preview(PaymentPlanPreviewRequest request);

    PaymentPlanPublishResponse publish(PaymentPlanPublishRequest request);

    PaymentPlanPublishResponse activate(Long catalogId);

    PaymentPlanPublishResponse retire(Long catalogId);
}
