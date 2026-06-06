package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_account")
public class LedgerAccountEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private String accountNo;
    private String accountType;
    private String currency;
    private BigDecimal balance;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private Integer status;
    @Version
    private Integer version;
    private String remark;
}
