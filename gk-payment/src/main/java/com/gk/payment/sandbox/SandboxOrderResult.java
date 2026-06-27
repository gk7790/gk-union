package com.gk.payment.sandbox;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "SandboxOrderResult", description = "沙箱 mock 回调结果")
public class SandboxOrderResult {
    @Schema(title = "订单类型", description = "PAY / PAYOUT")
    private String orderType;
    @Schema(title = "系统订单号")
    private String systemOrderNo;
    @Schema(title = "商户订单号")
    private String merchantOrderNo;
    @Schema(title = "订单状态")
    private String status;
    @Schema(title = "本次是否变更订单状态")
    private Boolean changed;
    @Schema(title = "说明")
    private String message;
    @Schema(title = "mock 结果", description = "SUCCESS / FAILED")
    private String outcome;
    @Schema(title = "失败原因")
    private String failReason;
    @Schema(title = "商户通知 HTTP 结果")
    private Map<String, Object> notify;

    public static SandboxOrderResult basic(String orderType,
                                           String systemOrderNo,
                                           String merchantOrderNo,
                                           String status,
                                           boolean changed,
                                           String message) {
        SandboxOrderResult result = new SandboxOrderResult();
        result.setOrderType(orderType);
        result.setSystemOrderNo(systemOrderNo);
        result.setMerchantOrderNo(merchantOrderNo);
        result.setStatus(status);
        result.setChanged(changed);
        result.setMessage(message);
        return result;
    }

    public SandboxOrderResult withMock(String outcome, String failReason, Map<String, Object> notify) {
        this.outcome = outcome;
        this.failReason = failReason;
        this.notify = notify;
        return this;
    }
}
