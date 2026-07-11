package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class MerchantBalanceDTO {
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "商户号")
    private String merchantNo;
    @Schema(title = "商户名称")
    private String merchantName;
    @Schema(title = "商户简称")
    private String merchantShortName;
    @Schema(title = "商户状态")
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
    @Schema(title = "结算模式")
    private String settleMode;
    @Schema(title = "结算周期")
    private String settleCycle;
    @Schema(title = "最小结算金额")
    private BigDecimal minSettleAmount;
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "可用余额")
    private BigDecimal availableBalance;
    @Schema(title = "可用余额展示")
    private String availableBalanceText;
    @Schema(title = "冻结余额")
    private BigDecimal frozenBalance;
    @Schema(title = "冻结余额展示")
    private String frozenBalanceText;
    @Schema(title = "待结算余额")
    private BigDecimal pendingSettleBalance;
    @Schema(title = "待结算余额展示")
    private String pendingSettleBalanceText;
    @Schema(title = "有效余额")
    private BigDecimal effectiveBalance;
    @Schema(title = "有效余额展示")
    private String effectiveBalanceText;
    @Schema(title = "总资产")
    private BigDecimal totalBalance;
    @Schema(title = "总资产展示")
    private String totalBalanceText;
    @Schema(title = "是否已初始化账本账户")
    private Boolean hasLedgerAccount;
    @Schema(title = "是否已绑定Telegram机器人群")
    private Boolean tgChatBound;
    @Schema(title = "最后入账时间")
    private Instant lastPostedAt;
}
