package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payout_route_attempt")
public class PayoutRouteAttemptEntity extends SimpleEntity {
    private Long tenantId;
    private Long payoutOrderId;
    private String payoutOrderNo;
    private Integer attemptNo;
    private Long routeOptionId;
    private Long routeRuleId;
    private Long routeGroupId;
    private Long routeChannelId;
    private Long pspId;
    private String pspCode;
    private Long pspMethodId;
    private String pspMethodCode;
    private Long pspAccountId;
    private String pspAccountNo;
    private String pspBankCode;
    private String submitResultStatus;
    private String errorCode;
    private String errorMessage;
    private String pspRequestNo;
    private String pspOrderNo;
    private String rawStatus;
    private Integer responseStatus;
    private String responseCode;
    private String responseMessage;
    private String rawResponseJson;
    private String remark;
}
