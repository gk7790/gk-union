package com.gk.infra.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "运行参数")
public class RuntimeParamDTO implements Serializable {

    @Schema(title = "参数编码")
    private String paramCode;

    @Schema(title = "参数值")
    private Object paramValue;

    @Schema(title = "默认值")
    private Object defaultValue;

    @Schema(title = "权限标识")
    private String auth;

    @Schema(title = "参数名称")
    private String name;

    @Schema(title = "备注")
    private String remark;
}
