package com.gk.infra.ipwhitelist.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class SysApiIpWhitelistDTO implements Serializable {
    private Long id;
    private String apiType;
    private Long tenantId;
    private Long merchantId;
    private String ruleName;
    private String ipPattern;
    private Integer status;
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
