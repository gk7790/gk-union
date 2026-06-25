package com.gk.psp.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PSP 订单请求模型�? * <p>
 * payment 调用 PSP 时统一使用该边界对象，PSP 不直接依�?payment 订单实体�? */
@Data
@Builder
public class PspOrderRequest {
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long merchantAppId;
    private String orderNo;
    private String merchantOrderNo;
    private String countryCode;
    private String currency;
    private String methodCode;
    private BigDecimal amount;
    private String returnUrl;
    private String payerJson;
    private String payeeName;
    private String payeeAccountNo;
    private String payeeBankCode;
    private String payeeWalletType;
    private String payeeJson;
    private String extraJson;
    private Long routeRuleId;
    private Long pspId;
    private String pspCode;
    private Long pspMethodId;
    private String pspMethodCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private String pspOrderNo;
}
