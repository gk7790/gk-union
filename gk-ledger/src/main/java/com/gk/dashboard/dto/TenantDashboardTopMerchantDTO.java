package com.gk.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(title = "租户首页Top商户")
public class TenantDashboardTopMerchantDTO {

    @Schema(title = "币种")
    private String currency;
    @Schema(title = "时间范围")
    private String range;
    @Schema(title = "排行列表")
    private List<TopMerchant> items;

    @Data
    @Schema(title = "Top商户")
    public static class TopMerchant {
        @Schema(title = "商户ID")
        private Long merchantId;
        @Schema(title = "商户")
        private String merchantNo;
        @Schema(title = "商户名称")
        private String merchantName;
        @Schema(title = "代收成功金额")
        private BigDecimal payInAmount;
        @Schema(title = "代付成功金额")
        private BigDecimal payOutAmount;
        @Schema(title = "合计成功金额")
        private BigDecimal totalAmount;
        @Schema(title = "代收成功笔数")
        private Long payInCount;
        @Schema(title = "代付成功笔数")
        private Long payOutCount;
    }
}
