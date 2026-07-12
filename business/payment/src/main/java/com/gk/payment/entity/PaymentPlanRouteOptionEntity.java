package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PSP candidate route under one payment plan bucket.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_plan_route_option")
public class PaymentPlanRouteOptionEntity extends BaseEntity {
    private Long tenantId;
    private Long catalogId;
    private Long bucketId;
    private Long routeRuleId;
    private Long routeGroupId;
    private Long routeChannelId;
    private Long pspId;
    private String pspCode;
    private Long pspMethodId;
    private String pspMethodCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private Long pspFeeRuleId;
    private String pspFeeSnapshotJson;
    private String routeRuleSnapshotJson;
    private String routeGroupSnapshotJson;
    private String routeChannelSnapshotJson;
    private String pspProviderSnapshotJson;
    private String pspMethodSnapshotJson;
    private String pspAccountSnapshotJson;
    private Integer priority;
    private Integer weight;
    private Integer fallbackOrder;
    private String status;
    private Integer sort;
}
