package com.gk.ledger.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psp_provider")
public class PspProviderEntity extends SimpleEntity {
    private String pspCode;
    private String pspName;
    private Integer status;
    private String baseUrl;
    private String configJson;
    private String secretJson;
    private String remark;
}
