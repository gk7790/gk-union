package com.gk.psp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
public class PspProviderDTO {
    private Long id;
    @Schema(title = "PSP编码")
    private String pspCode;
    @Schema(title = "PSP名称")
    private String pspName;
    @Schema(title = "主要国家编码")
    private String countryCode;
    @Schema(title = "状态")
    private Integer status;
    @Schema(title = "基础URL")
    private String baseUrl;
    @Schema(title = "API版本")
    private String apiVersion;
    @Schema(title = "是否支持代收")
    private Integer supportPayin;
    @Schema(title = "是否支持代付")
    private Integer supportPayout;
    @Schema(title = "扩展配置JSON")
    private String configJson;
    @Schema(title = "备注")
    private String remark;
    private Long createdBy;
    private Instant createdAt;
    private Long updatedBy;
    private Instant updatedAt;
}
