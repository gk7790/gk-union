package com.gk.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Telegram绑定验证码")
public class TgBindCodeDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "一次性绑定码")
    private String code;
    @Schema(title = "发起绑定的系统用户ID")
    private Long userId;
    @Schema(title = "主体ID")
    private Long subjectId;
    @Schema(title = "已使用时记录的TG用户ID")
    private Long tgUserId;
    @Schema(title = "状态", description = "0待使用 1已使用 2过期")
    private Integer status;
    @Schema(title = "过期时间")
    private Instant expireAt;
    @Schema(title = "使用时间")
    private Instant usedAt;
    @Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
}
