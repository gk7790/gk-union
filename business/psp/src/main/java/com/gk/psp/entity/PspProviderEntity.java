package com.gk.psp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("psp_provider")
public class PspProviderEntity extends SimpleEntity {
    private Long tenantId;
    private String pspCode;
    private String pspName;
    private String countryCode;
    private Integer status;
    private String baseUrl;
    private String apiVersion;
    private Integer supportPayin;
    private Integer supportPayout;
    private String configJson;
    private String remark;
}
