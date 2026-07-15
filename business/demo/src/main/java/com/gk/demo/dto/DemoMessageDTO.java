package com.gk.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Demo 消息")
public class DemoMessageDTO {
    @Schema(title = "编码")
    private String code;

    @Schema(title = "内容")
    private String message;
}
