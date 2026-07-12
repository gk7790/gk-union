package com.gk.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(name = "MerchantWalletBalanceDTO", description = "商户钱包余额（按币种聚合）")
public class MerchantWalletBalanceDTO {
    @Schema(title = "币种")
    private String currency;
    @Schema(title = "可用余额")
    private BigDecimal availableBalance;
    @Schema(title = "可用余额展示")
    private String availableBalanceText;
    @Schema(title = "待结算余额")
    private BigDecimal pendingSettleBalance;
    @Schema(title = "待结算余额展示")
    private String pendingSettleBalanceText;
    @Schema(title = "冻结余额")
    private BigDecimal frozenBalance;
    @Schema(title = "冻结余额展示")
    private String frozenBalanceText;
    @Schema(title = "总资产")
    private BigDecimal totalBalance;
    @Schema(title = "总资产展示")
    private String totalBalanceText;
    @Schema(title = "最后入账时间")
    private Instant lastPostedAt;
}
