package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.MerchantFeeRuleDTO;
import com.gk.payment.dto.MerchantFeeViewResponse;
import com.gk.payment.service.MerchantFeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商户手续费规")
@RestController
@RequestMapping("/payment/merchant-fee-rule")
@RequiredArgsConstructor
public class MerchantFeeRuleController {
    private final MerchantFeeRuleService merchantFeeRuleService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:merchant-fee-rule:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantFeeRuleDTO> page = merchantFeeRuleService.page(params);
        return R.ok(page);
    }

    @GetMapping("merchant-view")
    @Operation(summary = "商户手续费展")
    public R<MerchantFeeViewResponse> merchantView(@RequestMap DynMap params) {
        return R.ok(merchantFeeRuleService.merchantView(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('payment:merchant-fee-rule:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(merchantFeeRuleService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('payment:merchant-fee-rule:save')")
    public R<?> save(@RequestBody MerchantFeeRuleDTO dto) {
        merchantFeeRuleService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('payment:merchant-fee-rule:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody MerchantFeeRuleDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        merchantFeeRuleService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('payment:merchant-fee-rule:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        merchantFeeRuleService.delete(ids);
        return R.ok();
    }
}
