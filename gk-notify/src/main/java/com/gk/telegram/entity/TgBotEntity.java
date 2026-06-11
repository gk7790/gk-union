package com.gk.telegram.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gk.common.core.entity.SimpleEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Telegram机器人配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tg_bot")
public class TgBotEntity extends SimpleEntity {
    /** 归属: PLATFORM/TENANT */
    private String ownerScope;
    /** 租户ID; TENANT必填, PLATFORM为空 */
    private Long tenantId;
    /** 内部机器人编号 */
    private String botNo;
    /** Bot @username */
    private String username;
    /** Telegram BotUserId(getMe) */
    private Long botUserId;
    /** BotToken密文(对称加密, 禁止明文) */
    private String tokenCipher;
    /** Token哈希(查重/校验) */
    private String tokenHash;
    /** Webhook secret_token */
    private String secretToken;
    /** 已设置的Webhook地址 */
    private String webhookUrl;
    /** 模式: WEBHOOK/POLLING */
    private String mode;
    /** 状态: 0停用 1启用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
