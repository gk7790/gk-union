package com.gk.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PaymentRouteGroupDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "路由组编码")
    private String groupCode;
    @Schema(title = "路由组名称")
    private String groupName;
    @Schema(title = "交易方向: PAYIN/PAYOUT")
    private String direction;
    @Schema(title = "国家/地区编码")
    private String countryCode;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "平台统一支付方式编码")
    private String methodCode;
    @Schema(title = "组内策略")
    private String strategy;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
