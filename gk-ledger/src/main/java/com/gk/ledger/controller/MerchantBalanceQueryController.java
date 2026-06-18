package com.gk.ledger.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.ledger.dto.MerchantBalanceDTO;
import com.gk.ledger.service.MerchantBalanceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "租户商户余额")
@RestController
@RequestMapping("/ledger/merchant-balance")
@RequiredArgsConstructor
public class MerchantBalanceQueryController {
    private final MerchantBalanceQueryService merchantBalanceQueryService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "tenantId", description = "租户ID", in = ParameterIn.QUERY),
            @Parameter(name = "merchantNo", description = "商户号", in = ParameterIn.QUERY),
            @Parameter(name = "merchantName", description = "商户名称", in = ParameterIn.QUERY),
            @Parameter(name = "currency", description = "币种", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('ledger:merchant-balance:page')")
    public R<PageData<MerchantBalanceDTO>> page(@RequestMap DynMap params) {
        PageData<MerchantBalanceDTO> page = merchantBalanceQueryService.page(params);
        return R.ok(page);
    }
}
