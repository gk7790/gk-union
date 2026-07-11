package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.payment.dto.PayoutOrderDTO;
import com.gk.payment.notify.MerchantNotifyExecutor;
import com.gk.payment.sandbox.SandboxOrderResult;
import com.gk.payment.sandbox.SandboxOrderService;
import com.gk.payment.service.PayoutOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "代付订单")
@RestController
@RequestMapping("/payment/payout-order")
@RequiredArgsConstructor
public class PayoutOrderController {
    private final PayoutOrderService payoutOrderService;
    private final MerchantNotifyExecutor merchantNotifyExecutor;
    private final SandboxOrderService sandboxOrderService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:payout-order:page')")
    public R<?> page(@RequestMap DynMap params) {
        if (isMerchantSubject()) {
            return R.ok(payoutOrderService.merchantPage(params));
        }
        PageData<PayoutOrderDTO> page = payoutOrderService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('payment:payout-order:info')")
    public R<?> get(@PathVariable("id") Long id) {
        if (isMerchantSubject()) {
            return R.ok(payoutOrderService.merchantGet(id));
        }
        return R.ok(payoutOrderService.get(id));
    }

    private boolean isMerchantSubject() {
        return SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType());
    }

    @PostMapping("{id}/notify")
    @Operation(summary = "手动通知商户", description = "立即同步重发; 成功提示通知成功, 失败返回 msg, 详情见通知记录")
    @PreAuthorize("hasAuthority('payment:merchant-notify-task:resend')")
    public R<Void> notifyMerchant(@PathVariable("id") Long id) {
        AssertUtils.isNull(id, "id");
        return R.fromResult(merchantNotifyExecutor.resendPayoutOrder(id));
    }

    @PostMapping("{id}/sandbox/mock-callback")
    @Operation(summary = "沙箱 mock 回调", description = "测试应用订单模拟 PSP 终态回调，并同步 POST 商户 notify_url；无论商户应答是否成功，都返回完整结果供前端展示")
    @PreAuthorize("hasAuthority('payment:mock-callback:resend')")
    public R<SandboxOrderResult> sandboxMockCallback(@PathVariable("id") Long id, @RequestBody DynMap params) {
        AssertUtils.isNull(id, "id");
        return R.ok(sandboxOrderService.mockPayoutCallback(id, params));
    }
}
