package com.gk.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_method")
public class PaymentMethodEntity extends SimpleEntity {
    private String methodCode;
    private String methodName;
    private String methodType;
    private String direction;
    private String countryCode;
    private String currency;
    private Integer status;
    private Integer sort;
    private String iconUrl;
    private String remark;
}
