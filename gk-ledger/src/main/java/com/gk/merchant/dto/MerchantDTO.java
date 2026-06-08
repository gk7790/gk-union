package com.gk.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantDTO {
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户号")
    private String merchantNo;
    @Schema(title = "商户名称")
    private String merchantName;
    @Schema(title = "商户简称")
    private String merchantShortName;
    @Schema(title = "商户类型")
    private String merchantType;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "风控状态")
    private String riskStatus;
    @Schema(title = "国家编码")
    private String countryCode;
    @Schema(title = "默认币种")
    private String defaultCurrency;
    @Schema(title = "商户时区")
    private String timezone;
    @Schema(title = "商户语言")
    private String lang;
    @Schema(title = "联系人")
    private String contactName;
    @Schema(title = "联系邮箱")
    private String contactEmail;
    @Schema(title = "联系电话")
    private String contactPhone;
    @Schema(title = "营业执照/注册编号")
    private String businessLicenseNo;
    @Schema(title = "结算模式")
    private String settleMode;
    @Schema(title = "结算周期")
    private String settleCycle;
    @Schema(title = "最小结算金额")
    private BigDecimal minSettleAmount;
    @Schema(title = "商户扩展配置JSON")
    private String configJson;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建人ID")
    private Long createdBy;
    @Schema(title = "创建时间")
    private Instant createdAt;
    @Schema(title = "更新人ID")
    private Long updatedBy;
    @Schema(title = "更新时间")
    private Instant updatedAt;
}
