package com.gk.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantFeeViewResponse {
    private Long tenantId;
    private Long merchantId;
    private String merchantName;
    private String countryCode;
    private String currency;
    private List<Group> groups = new ArrayList<>();

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Group {
        private String direction;
        private String directionName;
        private List<Item> items = new ArrayList<>();
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private Long ruleId;
        private String ruleName;
        private String direction;
        private String directionName;
        private String countryCode;
        private String currency;
        private String methodCode;
        private String methodName;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String feeMode;
        private String feeModeName;
        private BigDecimal feeRate;
        private String feeRateText;
        private BigDecimal feeFixed;
        private String feeFixedText;
        private BigDecimal minFee;
        private String minFeeText;
        private BigDecimal maxFee;
        private String maxFeeText;
        private String feeText;
        private String feeBearer;
        private String settleMode;
        private Integer priority;
        private Instant effectiveAt;
        private Instant expireAt;
        private Integer status;
        private String remark;
    }
}
