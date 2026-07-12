package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodDTO {
    private Long id;
    @Schema(description = "系统支付方式编码，如 MAYA、BANK_CARD")
    private String methodCode;
    @Schema(description = "支付方式名称")
    private String methodName;
    @Schema(description = "类型: WALLET/BANK_CARD/QR/CASH/CARD")
    private String methodType;
    @Schema(description = "方向: PAYIN/PAYOUT/BOTH，空表示不限")
    private String direction;
    @Schema(description = "国家/地区，空表示通用")
    private String countryCode;
    @Schema(description = "币种，空表示通用")
    private String currency;
    @Schema(description = "状 1正常 2暂停 3停用")
    private Integer status;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "图标")
    private String iconUrl;
    @Schema(description = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
