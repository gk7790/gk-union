package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * Telegram绑定验证码(一次性)
 * <p>无 created_by/updated_by 列, 故不继承 BaseEntity</p>
 */
@Data
@TableName("tg_bind_code")
public class TgBindCodeEntity implements Serializable {
    @TableId
    private Long id;
    /** 租户ID */
    private Long tenantId;
    /** 一次性绑定码 */
    private String code;
    /** 发起绑定的系统用户ID */
    private Long userId;
    /** 主体ID */
    private Long subjectId;
    /** 已使用时记录的TG用户ID */
    private Long tgUserId;
    /** 状态: 0待使用 1已使用 2过期 */
    private Integer status;
    /** 过期时间 */
    private Instant expireAt;
    /** 使用时间 */
    private Instant usedAt;
    /** 创建时间 */
    private Instant createdAt;
}
