package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Telegram出站消息任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tg_message_task")
public class TgMessageTaskEntity extends SimpleEntity {
    /** 租户ID */
    private Long tenantId;
    /** 商户ID */
    private Long merchantId;
    /** 机器人ID, 关联tg_bot.id */
    private Long botId;
    /** 目标ChatId */
    private Long chatId;
    /** 消息任务编号 */
    private String taskNo;
    /** 业务类型: PAYIN_ORDER/PAYOUT_ORDER等 */
    private String bizType;
    /** 业务编号 */
    private String bizNo;
    /** 触发事件 */
    private String eventType;
    /** 来源Outbox事件ID(幂等) */
    // 来源事件ID用于和业务 outbox 做幂等关联，避免同一事件重复生成发送任务。
    private String sourceEventId;
    /** 解析模式: MarkdownV2/HTML/NONE */
    private String parseMode;
    /** 最终发送内容，已按 parseMode 渲染完成 */
    private String content;
    /** 扩展消息载荷，如 reply_markup 按钮等 */
    private String payloadJson;
    /** 状态: INIT/PROCESSING/SUCCESS/FAILED/DEAD */
    // 任务状态驱动发送机调度，失败达到上限后进入 DEAD。
    private String status;
    /** 已重试次数 */
    private Integer retryCount;
    /** 最大重试次数 */
    private Integer maxRetryCount;
    /** 下次重试时间 */
    private Instant nextRetryAt;
    /** 发送成功后TG返回的message_id */
    private Long tgMessageId;
    /** 最后TG错误码(如429限流) */
    private Integer lastErrorCode;
    /** 最后错误信息 */
    private String lastErrorMsg;
    /** 最后尝试时间 */
    private Instant lastAttemptAt;
    /** 锁定节点 */
    // 多节点发送时用 lockedBy + lockUntil 抢占任务，避免并发重复发送。
    private String lockedBy;
    /** 锁定过期时间 */
    private Instant lockUntil;
    /** 成功时间 */
    private Instant successAt;
    /** 进入死信时间 */
    private Instant deadAt;
    /** 链路追踪ID */
    private String traceId;
}
