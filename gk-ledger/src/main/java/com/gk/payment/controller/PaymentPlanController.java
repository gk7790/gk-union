package com.gk.payment.controller;

import com.gk.common.model.R;
import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.service.PaymentPlanAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment Plan")
@RestController
@RequestMapping("/payment/payment-plan")
@RequiredArgsConstructor
public class PaymentPlanController {
    private final PaymentPlanAdminService paymentPlanAdminService;

    /**
     * Preview the full payment plan compiled from current configuration.
     */
    @PostMapping("preview")
    @Operation(summary = "Payment plan preview")
    @PreAuthorize("hasAuthority('payment:payment-plan:preview')")
    public R<?> preview(@RequestBody PaymentPlanPreviewRequest request) {
        return R.ok(paymentPlanAdminService.preview(request));
    }

    /**
     * Publish a compiled payment plan and evict the active-plan cache.
     */
    @PostMapping("publish")
    @Operation(summary = "Payment plan publish")
    @PreAuthorize("hasAuthority('payment:payment-plan:publish')")
    public R<?> publish(@RequestBody PaymentPlanPublishRequest request) {
        return R.ok(paymentPlanAdminService.publish(request));
    }
}
