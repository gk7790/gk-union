package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.PaymentRouteGroupDTO;
import com.gk.payment.service.PaymentRouteGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment route group")
@RestController
@RequestMapping("/payment/route-group")
@RequiredArgsConstructor
public class PaymentRouteGroupController {
    private final PaymentRouteGroupService paymentRouteGroupService;

    @GetMapping("page")
    @Operation(summary = "Page")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "Page number, starts from 1", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "Page size", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "Order field", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "Order direction: asc or desc", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:route-group:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PaymentRouteGroupDTO> page = paymentRouteGroupService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}/check")
    @Operation(summary = "Check route group coverage")
    @PreAuthorize("hasAuthority('payment:route-group:check')")
    public R<?> check(@PathVariable("id") Long id) {
        return R.ok(paymentRouteGroupService.check(id));
    }

    @GetMapping("{id}")
    @Operation(summary = "Info")
    @PreAuthorize("hasAuthority('payment:route-group:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(paymentRouteGroupService.get(id));
    }

    @PostMapping
    @Operation(summary = "Save")
    @PreAuthorize("hasAuthority('payment:route-group:save')")
    public R<?> save(@RequestBody PaymentRouteGroupDTO dto) {
        paymentRouteGroupService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "Update")
    @PreAuthorize("hasAuthority('payment:route-group:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PaymentRouteGroupDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        paymentRouteGroupService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "Delete")
    @PreAuthorize("hasAuthority('payment:route-group:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        paymentRouteGroupService.delete(ids);
        return R.ok();
    }
}
