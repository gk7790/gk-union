package com.gk.psp.dispatch;

import com.gk.psp.enums.PspPayoutSubmitResultStatus;
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
    private PspPayoutSubmitResultStatus submitResultStatus;

    public boolean isAccepted() {
        return PspPayoutSubmitResultStatus.ACCEPTED.equals(submitResultStatus);
    }

    public boolean isRejected() {
        return PspPayoutSubmitResultStatus.REJECTED.equals(submitResultStatus);
    }

    public boolean isUnknown() {
        return submitResultStatus == null || PspPayoutSubmitResultStatus.UNKNOWN.equals(submitResultStatus);
    }
}
