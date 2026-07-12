package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("order_status_log")
public class OrderStatusLogEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private String logNo;
    private String direction;
    private Long orderId;
    private String orderNo;
    private String fromStatus;
    private String toStatus;
    private String eventType;
    private String reason;
    private String operatorType;
    private String operatorId;
    private String requestId;
    private String traceId;
    private String metadataJson;
}
