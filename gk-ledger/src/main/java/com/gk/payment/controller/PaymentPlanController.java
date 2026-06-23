package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.payment.dto.PaymentPlanPreviewRequest;
import com.gk.payment.dto.PaymentPlanPublishRequest;
import com.gk.payment.service.PaymentPlanAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("page")
    @Operation(summary = "Payment plan version page")
    @PreAuthorize("hasAuthority('payment:payment-plan:page')")
    public R<?> page(@RequestMap DynMap params) {
        if (params == null) {
            params = DynMap.empty();
        }
        return R.ok(paymentPlanAdminService.page(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "Payment plan version detail")
    @PreAuthorize("hasAuthority('payment:payment-plan:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(paymentPlanAdminService.detail(id));
    }

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

    @PostMapping("{id}/activate")
    @Operation(summary = "Payment plan activate")
    @PreAuthorize("hasAuthority('payment:payment-plan:activate')")
    public R<?> activate(@PathVariable("id") Long id) {
        return R.ok(paymentPlanAdminService.activate(id));
    }

    @PostMapping("{id}/retire")
    @Operation(summary = "Payment plan retire")
    @PreAuthorize("hasAuthority('payment:payment-plan:retire')")
    public R<?> retire(@PathVariable("id") Long id) {
        return R.ok(paymentPlanAdminService.retire(id));
    }
}
