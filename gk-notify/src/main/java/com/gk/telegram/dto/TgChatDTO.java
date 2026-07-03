package com.gk.telegram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Telegram 会话/群组绑定前后端传输对象。
 * <p>
 * 用于后台查看群绑定关系、通知用途和订阅事件范围。
 */
@Data
@Schema(description = "Telegram会话/群组")
public class TgChatDTO {
    @Schema(title = "主键ID")
    private Long id;
    @Schema(title = "租户ID")
    private Long tenantId;
    @Schema(title = "商户ID")
    private Long merchantId;
    @Schema(title = "所属机器人ID")
    private Long botId;
    @Schema(title = "机器人名称", description = "优先返回用户名，没有用户名时返回机器人编号")
    private String botName;
    @Schema(title = "Telegram ChatId", description = "群为负数")
    private Long chatId;
    @Schema(title = "类型", description = "PRIVATE/GROUP/SUPERGROUP/CHANNEL")
    private String chatType;
    @Schema(title = "群/频道名称")
    private String title;
    @Schema(title = "用途", description = "NOTIFY/OPS/CUSTOMER")
    private String purpose;
    @Schema(title = "订阅事件", description = "英文逗号分隔，空=不接收任何通知")
    private String eventTypes;
    @Schema(title = "消息语言")
    private String lang;
    @Schema(title = "状态", description = "0停用 1启用")
    private Integer status;
    @Schema(title = "备注")
    private String remark;
    @Schema(title = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;
    @Schema(title = "更新时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;
}
