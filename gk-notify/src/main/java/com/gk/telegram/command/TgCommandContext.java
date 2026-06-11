package com.gk.telegram.command;

import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgBotEntity;
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
    /** 原始文本 */
    private String rawText;
    /** 命令(小写, 含前导/, 如 /balance) */
    private String command;
    /** 命令参数(空格切分, 不含命令本身) */
    private List<String> args;
    /** 已解析的账号绑定, 未绑定为null */
    private TgAccountEntity account;

    public String arg(int index) {
        if (args == null || index < 0 || index >= args.size()) {
            return null;
        }
        return args.get(index);
    }
}
