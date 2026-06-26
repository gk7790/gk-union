package com.gk.psp.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PspProviderDTO {
    private Long id;
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
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
