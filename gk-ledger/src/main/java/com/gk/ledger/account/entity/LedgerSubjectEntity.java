package com.gk.ledger.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ledger_subject")
public class LedgerSubjectEntity extends SimpleEntity {
    private Long tenantId;
    private Long merchantId;
    private Long pspId;
    private String subjectNo;
    private String subjectType;
    private String subjectName;
    private Integer status;
    private String remark;
}
