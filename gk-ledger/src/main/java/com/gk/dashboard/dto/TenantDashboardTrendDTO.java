package com.gk.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(title = "租户首页趋势")
public class TenantDashboardTrendDTO {

    @Schema(title = "币种")
    private String currency;
    @Schema(title = "时间范围")
    private String range;
    @Schema(title = "时区")
    private String timezone;
    @Schema(title = "趋势")
    private List<TrendPoint> points;

    @Data
    @Schema(title = "日趋")
    public static class TrendPoint {
        @Schema(title = "日期 yyyy-MM-dd")
        private String date;
        @Schema(title = "代收成功金额")
        private BigDecimal payInAmount;
        @Schema(title = "代付成功金额")
        private BigDecimal payOutAmount;
        @Schema(title = "代收成功笔数")
        private Long payInCount;
        @Schema(title = "代付成功笔数")
        private Long payOutCount;
    }
}
