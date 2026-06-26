package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.PayOrderDTO;
import com.gk.payment.notify.MerchantNotifyExecutor;
import com.gk.payment.service.PayOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "代收订单")
@RestController
@RequestMapping("/payment/pay-order")
@RequiredArgsConstructor
public class PayOrderController {
    private final PayOrderService payOrderService;
    private final MerchantNotifyExecutor merchantNotifyExecutor;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:pay-order:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PayOrderDTO> page = payOrderService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('payment:pay-order:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(payOrderService.get(id));
    }

    @PostMapping("{id}/release-settle")
    @Operation(summary = "手动释放待结", description = "将代收成功且 settle_status=PENDING 的订单释放至商户可用余额")
    @PreAuthorize("hasAuthority('payment:pay-order:release-settle')")
    public R<Void> releaseSettle(@PathVariable("id") Long id) {
        AssertUtils.isNull(id, "id");
        payOrderService.releaseSettle(id);
        return R.ok();
    }

    @PostMapping("{id}/notify")
    @Operation(summary = "手动通知商户", description = "立即同步重发; 成功提示通知成功, 失败返回 msg, 详情见通知记录")
    @PreAuthorize("hasAuthority('payment:merchant-notify-task:resend')")
    public R<Void> notifyMerchant(@PathVariable("id") Long id) {
        AssertUtils.isNull(id, "id");
        return R.fromResult(merchantNotifyExecutor.resendPayOrder(id));
    }
}
