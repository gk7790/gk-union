package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * Telegram入站更新日志(幂等 + 审计)
 * <p>无 created_by/updated_by 列, 故不继承 BaseEntity</p>
 */
@Data
@TableName("tg_update_log")
public class TgUpdateLogEntity implements Serializable {
    /** 主键ID */
    @TableId
    private Long id;
    /** 机器人ID, 关联tg_bot.id */
    private Long botId;
    /** Telegram update_id(幂等键) */
    private Long updateId;
    /** 来源TG用户ID */
    private Long tgUserId;
    /** 来源ChatId */
    private Long chatId;
    /** 类型: message/callback_query等 */
    private String updateType;
    /** 命令(如/balance /order) */
    private String command;
    /** 原始Update报文 */
    private String rawJson;
    /** 处理状态: 0待处理 1成功 2失败 */
    private Integer handleStatus;
    /** 处理错误信息 */
    private String errorMsg;
    /** 创建时间 */
    private Instant createdAt;
}
