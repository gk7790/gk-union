package com.gk.payment.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.payment.dto.MerchantNotifyTaskDTO;
import com.gk.payment.notify.MerchantNotifyExecutor;
import com.gk.payment.service.MerchantNotifyTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商户通知任务")
@RestController
@RequestMapping("/payment/merchant-notify-task")
@RequiredArgsConstructor
public class MerchantNotifyTaskController {
    private final MerchantNotifyTaskService merchantNotifyTaskService;
    private final MerchantNotifyExecutor merchantNotifyExecutor;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('payment:merchant-notify-task:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantNotifyTaskDTO> page = merchantNotifyTaskService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('payment:merchant-notify-task:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(merchantNotifyTaskService.get(id));
    }

    @PostMapping("{id}/resend")
    @Operation(summary = "手动重发", description = "立即同步重发该商户通知一 成功提示通知成功, 失败返回 msg, 详情见通知记录")
    @PreAuthorize("hasAuthority('payment:merchant-notify-task:resend')")
    public R<Void> resend(@PathVariable("id") Long id) {
        return R.fromResult(merchantNotifyExecutor.resend(id));
    }
}
