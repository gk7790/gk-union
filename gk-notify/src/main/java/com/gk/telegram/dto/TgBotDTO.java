package com.gk.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Telegram机器人")
public class TgBotDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "归属", description = "PLATFORM/TENANT")
    private String ownerScope;
    @Schema(title = "租户ID", description = "TENANT必填, PLATFORM为空")
    private Long tenantId;
    @Schema(title = "内部机器人编号", description = "新增时自动生成", accessMode = Schema.AccessMode.READ_ONLY)
    private String botNo;
    @Schema(title = "Bot @username", description = "新增可留空, 测试连通(getMe)后回填")
    private String username;
    @Schema(title = "Telegram BotUserId", accessMode = Schema.AccessMode.READ_ONLY)
    private Long botUserId;
    @Schema(title = "Bot Token", description = "仅新增/更新时提交明文, 加密落库后不再返回", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String token;
    @Schema(title = "Webhook地址", accessMode = Schema.AccessMode.READ_ONLY)
    private String webhookUrl;
    @Schema(title = "模式", description = "WEBHOOK/POLLING")
    private String mode;
    @Schema(title = "状态", description = "0停用 1启用")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
    @Schema(title = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}
