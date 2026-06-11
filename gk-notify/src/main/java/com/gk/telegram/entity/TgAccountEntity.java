package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * Telegram账号绑定(TG用户 ↔ 系统主体)
 * <p>无 created_by/updated_by 列, 故不继承 BaseEntity</p>
 */
@Data
@TableName("tg_account")
public class TgAccountEntity implements Serializable {
    @TableId
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 绑定时所用机器人ID, 关联tg_bot.id */
    private Long botId;
    /** Telegram用户ID */
    private Long tgUserId;
    /** TG用户名@ */
    private String tgUsername;
    /** 系统用户ID, 关联sys_user.id */
    private Long userId;
    /** 主体ID, 关联sys_user_subject.id */
    private Long subjectId;
    /** TG语言 */
    private String languageCode;
    /** 状态: 0解绑 1已绑定 */
    private Integer status;
    /** 绑定时间 */
    private Instant boundAt;
    /** 创建时间 */
    private Instant createdAt;
    /** 更新时间 */
    private Instant updatedAt;
}
