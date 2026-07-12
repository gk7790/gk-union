package com.gk.psp.query;

import com.gk.psp.callback.model.PspCallbackResult;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PspOrderQueryResult {
    private boolean success;
    private String pspCode;
    private String bizType;
    private String systemOrderNo;
    private String merchantOrderNo;
    private String pspOrderNo;
    private String pspStatus;
    private String orderStatus;
    private BigDecimal amount;
    private String currency;
    private String pspRequestNo;
    private String requestUrl;
    private String httpMethod;
    private String requestHeadersJson;
    private String requestBody;
    private Integer responseStatus;
    private String responseCode;
    private String responseMessage;
    private String responseSign;
    private String rawResponseJson;
    private String errorCode;
    private String errorMessage;

    public PspCallbackResult toCallbackResult(String fallbackBizType) {
        PspCallbackResult result = new PspCallbackResult();
        result.setPspCode(pspCode);
        result.setBizType(bizType == null ? fallbackBizType : bizType);
        result.setSystemOrderNo(systemOrderNo);
        result.setMerchantOrderNo(merchantOrderNo);
        result.setPspOrderNo(pspOrderNo);
        result.setPspStatus(pspStatus);
        result.setOrderStatus(orderStatus);
        result.setAmount(amount);
        result.setCurrency(currency);
        result.setCallbackType("QUERY");
        result.setSignature(responseSign);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
