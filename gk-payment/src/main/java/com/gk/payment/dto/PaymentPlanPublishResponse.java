package com.gk.payment.dto;

import lombok.Data;

@Data
public class PaymentPlanPublishResponse {
    private Boolean published;
    private Long catalogId;
    private Long versionNo;
    private String status;
    private Integer bucketCount;
    private Integer routeOptionCount;
    private Boolean redisEvicted;
}
