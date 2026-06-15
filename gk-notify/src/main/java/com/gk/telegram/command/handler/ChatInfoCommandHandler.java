package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.support.TgHtml;
import org.springframework.stereotype.Component;

/**
 * /chatinfo 指令处理器。
 * <p>展示当前 Telegram chat 的基础信息，以及该群/会话是否已绑定到系统商户。</p>
 */
@Component
public class ChatInfoCommandHandler implements TgCommandHandler {
    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/chatinfo";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "查看当前群信息和绑定状态";
    }

    /**
     * 查看会话信息本身不要求绑定。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 输出当前 Telegram chat 的基础信息，以及本地 tg_chat 绑定状态。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        StringBuilder sb = new StringBuilder(TgHtml.bold("Telegram 会话")).append("\n")
                .append("Chat ID: ").append(TgHtml.code(ctx.getChatId())).append("\n")
                .append("类型: ").append(TgHtml.code(ctx.getChatType())).append("\n")
                .append("标题: ").append(TgHtml.escape(ctx.getChatTitle())).append("\n");
        TgChatEntity chat = ctx.getChat();
        // ctx.chat 由 webhook 层按 bot_id + chat_id 查询，存在即说明当前会话已绑定。
        if (chat == null) {
            return sb.append("绑定状态: ").append(TgHtml.bold("未绑定")).toString();
        }
        return sb.append("绑定状态: ").append(TgHtml.bold("已绑定")).append("\n")
                .append("租户ID: ").append(TgHtml.code(chat.getTenantId())).append("\n")
                .append("商户ID: ").append(TgHtml.code(chat.getMerchantId())).append("\n")
                .append("用途: ").append(TgHtml.escape(chat.getPurpose()))
                .toString();
    }
}
