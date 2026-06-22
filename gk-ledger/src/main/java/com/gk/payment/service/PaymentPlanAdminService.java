package com.gk.payment.service;

import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPreviewResponse;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.dto.PaymentPlanPublishResponse;

public interface PaymentPlanAdminService {
    PaymentPlanPreviewResponse preview(PaymentPlanPreviewRequest request);

    PaymentPlanPublishResponse publish(PaymentPlanPublishRequest request);
}
