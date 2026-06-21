package com.gk.payment.controller;

import com.gk.common.model.R;
import com.gk.payment.dto.PayinConfigPrecheckRequest;
import com.gk.payment.plan.PayinPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "代收配置预检")
@RestController
@RequestMapping("/payment/payin-config")
@RequiredArgsConstructor
public class PayinConfigPrecheckController {
    private final PayinPlanService payinPlanService;

    /**
     * 后台配置发布前调用，提前验证代收完整链路是否能解析出可用方案。
     */
    @PostMapping("precheck")
    @Operation(summary = "代收配置发布预检")
    @PreAuthorize("hasAuthority('payment:payin-config:precheck')")
    public R<?> precheck(@RequestBody PayinConfigPrecheckRequest request) {
        return R.ok(payinPlanService.precheck(request));
    }
}
