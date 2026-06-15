package com.gk.telegram.command;

import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.entity.TgChatEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 入站指令上下文
 */
@Data
@Builder
public class TgCommandContext {
    /** 接收指令的机器人 */
    private TgBotEntity bot;
    /** 发送者TG用户ID */
    private Long tgUserId;
    /** 发送者TG用户名 */
    private String tgUsername;
    /** 发送者TG语言 */
    private String languageCode;
    /** 会话ID */
    private Long chatId;
    /** Telegram 会话类型: private/group/supergroup/channel。 */
    private String chatType;
    /** 群、超级群或频道标题；私聊通常为空。 */
    private String chatTitle;
    /** 原始文本 */
    private String rawText;
    /** 命令(小写, 含前导/, 如 /balance) */
    private String command;
    /** 命令参数(空格切分, 不含命令本身) */
    private List<String> args;
    /** 已解析的账号绑定, 未绑定为null */
    private TgAccountEntity account;
    /** 当前会话的群绑定记录；未绑定时为空。 */
    private TgChatEntity chat;
    /** 当前指令允许访问的租户范围，个人绑定来自 sys_user_subject，群绑定来自 tg_chat。 */
    private Long targetTenantId;
    /** 当前指令允许访问的商户范围，个人绑定来自 sys_user_subject，群绑定来自 tg_chat。 */
    private Long targetMerchantId;

    /**
     * 获取第 index 个命令参数，越界时返回 null。
     */
    public String arg(int index) {
        if (args == null || index < 0 || index >= args.size()) {
            return null;
        }
        return args.get(index);
    }

    /**
     * 判断当前上下文是否已通过个人或群任一方式绑定系统主体。
     */
    public boolean hasBoundTarget() {
        return account != null || chat != null;
    }

    /**
     * 获取查询类指令使用的租户范围，优先个人绑定，其次群绑定。
     */
    public Long targetTenantId() {
        if (targetTenantId != null) {
            return targetTenantId;
        }
        if (account != null && account.getTenantId() != null) {
            return account.getTenantId();
        }
        return chat == null ? null : chat.getTenantId();
    }

    /**
     * 获取查询类指令使用的商户范围，目前主要来自群绑定。
     */
    public Long targetMerchantId() {
        if (targetMerchantId != null) {
            return targetMerchantId;
        }
        return chat == null ? null : chat.getMerchantId();
    }

    /**
     * 当前消息是否来自 Telegram 私聊。
     */
    public boolean isPrivateChat() {
        return "private".equalsIgnoreCase(chatType);
    }

    /**
     * 当前消息是否来自 Telegram 群或超级群。
     */
    public boolean isGroupChat() {
        return "group".equalsIgnoreCase(chatType) || "supergroup".equalsIgnoreCase(chatType);
    }
}
