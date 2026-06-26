package com.gk.adjustment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.adjustment.dto.MerchantBalanceAdjustOrderDTO;
import com.gk.adjustment.service.MerchantBalanceAdjustOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商户余额调整")
@RestController
@RequestMapping("/ledger/merchant-balance-adjust")
@RequiredArgsConstructor
public class MerchantBalanceAdjustOrderController {
    private final MerchantBalanceAdjustOrderService merchantBalanceAdjustOrderService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('ledger:merchant-balance-adjust:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantBalanceAdjustOrderDTO> page = merchantBalanceAdjustOrderService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('ledger:merchant-balance-adjust:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(merchantBalanceAdjustOrderService.get(id));
    }

    @PostMapping("submit")
    @Operation(summary = "提交余额调整")
    @PreAuthorize("hasAuthority('ledger:merchant-balance-adjust:submit')")
    public R<?> submit(@RequestBody MerchantBalanceAdjustOrderDTO dto) {
        return R.ok(merchantBalanceAdjustOrderService.submit(dto));
    }
}
