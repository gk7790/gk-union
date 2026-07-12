package com.gk.infra.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(title = "运行参数更新")
public class RuntimeParamUpdateDTO implements Serializable {

    @Schema(title = "参数值")
    private Object paramValue;
}
