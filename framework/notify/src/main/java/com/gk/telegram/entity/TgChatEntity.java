package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Telegram会话/群组(内嵌事件订阅)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tg_chat")
public class TgChatEntity extends SimpleEntity {
    /** 租户ID */
    private Long tenantId;
    /** 商户ID(可选, 精细到商户) */
    private Long merchantId;
    /** 所属机器人ID, 关联tg_bot.id */
    private Long botId;
    /** Telegram ChatId(群为负数) */
    // 群组/supergroup 的 chatId 通常是负数，不能按普通用户ID处理。
    private Long chatId;
    /** 类型: PRIVATE/GROUP/SUPERGROUP/CHANNEL */
    private String chatType;
    /** 群/频道名称 */
    private String title;
    /** 用途: NOTIFY/OPS/CUSTOMER */
    private String purpose;
    /** 订阅事件(逗号分隔, 空=不接收) */
    private String eventTypes;
    /** 消息语言 */
    private String lang;
    /** 状态: 0停用 1启用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
