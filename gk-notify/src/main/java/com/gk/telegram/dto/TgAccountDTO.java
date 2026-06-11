package com.gk.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Telegram账号绑定")
public class TgAccountDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "机器人ID")
    private Long botId;
    @Schema(title = "Telegram用户ID")
    private Long tgUserId;
    @Schema(title = "TG用户名@")
    private String tgUsername;
    @Schema(title = "系统用户ID")
    private Long userId;
    @Schema(title = "主体ID")
    private Long subjectId;
    @Schema(title = "TG语言")
    private String languageCode;
    @Schema(title = "状态", description = "0解绑 1已绑定")
    private Integer status;
    @Schema(title = "绑定时间")
    private Instant boundAt;
    @Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
    @Schema(title = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}
