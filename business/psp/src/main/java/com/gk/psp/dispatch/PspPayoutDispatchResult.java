package com.gk.psp.dispatch;

import com.gk.psp.enums.PspPayoutSubmitStatus;
import lombok.Data;

@Data
public class PspPayoutDispatchResult {
    private boolean success;
    private String pspRequestNo;
    private String requestUrl;
    private String httpMethod;
    private String requestHeadersJson;
    private String requestBody;
    private Integer responseStatus;
    private String pspOrderNo;
    private String pspMerchantOrderNo;
    private String rawStatus;
    private String responseCode;
    private String responseMessage;
    private String responseSign;
    private String rawResponseJson;
    private String errorCode;
    private String errorMessage;
    private PspPayoutSubmitStatus submitResultStatus;

    /**
     * PSP 已受理提交请求，最终成功或失败仍以回调/查单为准。
     */
    public boolean isAccepted() {
        return PspPayoutSubmitStatus.ACCEPTED.equals(submitResultStatus);
    }

    /**
     * PSP 明确拒绝订单本身，当前代付订单可以进入失败终态。
     */
    public boolean isRejected() {
        return PspPayoutSubmitStatus.REJECTED.equals(submitResultStatus);
    }

    /**
     * 当前 PSP 路由资源不可用，但其它路由仍可能可用。
     */
    public boolean isRouteUnavailable() {
        return PspPayoutSubmitStatus.ROUTE_UNAVAILABLE.equals(submitResultStatus);
    }

    /**
     * PSP 提交结果无法确认，应保持冻结资金并等待查单/回调。
     */
    public boolean isUnknown() {
        return submitResultStatus == null || PspPayoutSubmitStatus.UNKNOWN.equals(submitResultStatus);
    }

}
