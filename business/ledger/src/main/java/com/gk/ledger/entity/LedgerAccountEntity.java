package com.gk.ledger.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账务账户
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("ledger_account")
public class LedgerAccountEntity extends SimpleEntity {
    private Long tenantId;
    private String accountNo;
    private String ownerType;
    private Long ownerId;
    private String accountType;
    private String currency;
    private String normalSide;
    private Integer allowNegative;
    private Integer status;
    private String remark;
}
