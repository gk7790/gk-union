package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_route_group")
public class PaymentRouteGroupEntity extends SimpleEntity {
    private Long tenantId;
    private String groupCode;
    private String groupName;
    private String direction;
    private String countryCode;
    private String currency;
    private String methodCode;
    private String strategy;
    private Integer status;
    private String remark;
}
