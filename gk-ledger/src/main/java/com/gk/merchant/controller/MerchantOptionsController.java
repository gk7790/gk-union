package com.gk.merchant.controller;

import com.gk.common.model.R;
import com.gk.merchant.dto.MerchantOptionsDTO;
import com.gk.merchant.service.MerchantOptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商户管理")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantOptionsController {

    private final MerchantOptionsService merchantOptionsService;

    @GetMapping("options")
    @Operation(summary = "商户主体范围下拉选项")
    public R<MerchantOptionsDTO> subjectOptions() {
        return R.ok(merchantOptionsService.options());
    }
}
