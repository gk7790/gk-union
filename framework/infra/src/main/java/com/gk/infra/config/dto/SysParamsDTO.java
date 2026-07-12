package com.gk.infra.config.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参数管理
 * @author Lowen
 * @since 1.0.0
 */
@Data
@Schema(title = "参数管理")
public class SysParamsDTO implements Serializable {

    @Schema(title = "id")
    private Long id;

    @Schema(title = "参数编码")
    private String paramCode;

    @Schema(title = "参数值")
    private String paramValue;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建时间")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(title = "更新时间")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

}
