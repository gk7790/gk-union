package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.PaymentRouteChannelDTO;
import com.gk.payment.service.PaymentRouteChannelService;
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

@Tag(name = "Payment route channel")
@RestController
@RequestMapping("/payment/route-channel")
@RequiredArgsConstructor
public class PaymentRouteChannelController {
    private final PaymentRouteChannelService paymentRouteChannelService;

    @GetMapping("page")
    @Operation(summary = "Page")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "Page number, starts from 1", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "Page size", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "Order field", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "Order direction: asc or desc", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:route-channel:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PaymentRouteChannelDTO> page = paymentRouteChannelService.page(params);
        return R.ok(page);
    }

    @GetMapping("options")
    @Operation(summary = "Route channel cascade options")
    @PreAuthorize("hasAuthority('payment:route-channel:options')")
    public R<?> options(@RequestMap DynMap params) {
        return R.ok(paymentRouteChannelService.options(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "Info")
    @PreAuthorize("hasAuthority('payment:route-channel:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(paymentRouteChannelService.get(id));
    }

    @PostMapping
    @Operation(summary = "Save")
    @PreAuthorize("hasAuthority('payment:route-channel:save')")
    public R<?> save(@RequestBody PaymentRouteChannelDTO dto) {
        paymentRouteChannelService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "Update")
    @PreAuthorize("hasAuthority('payment:route-channel:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PaymentRouteChannelDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        paymentRouteChannelService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "Delete")
    @PreAuthorize("hasAuthority('payment:route-channel:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        paymentRouteChannelService.delete(ids);
        return R.ok();
    }
}
