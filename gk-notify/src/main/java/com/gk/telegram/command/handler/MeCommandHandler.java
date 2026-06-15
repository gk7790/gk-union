package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgAccountEntity;
import com.gk.telegram.entity.TgChatEntity;
import com.gk.telegram.support.TgHtml;
import org.springframework.stereotype.Component;

/**
 * /me 指令处理器。
 * <p>展示当前 Telegram 用户绑定状态，同时在群聊中补充当前群绑定状态。</p>
 */
@Component
public class MeCommandHandler implements TgCommandHandler {
    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/me";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "查看当前 Telegram 绑定状态";
    }

    /**
     * 查看自身状态不要求提前绑定。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 输出当前 Telegram 用户绑定状态；群里调用时也会附带当前群绑定状态。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        StringBuilder sb = new StringBuilder(TgHtml.bold("Telegram 用户")).append("\n")
                .append("Telegram ID: ").append(TgHtml.code(ctx.getTgUserId())).append("\n")
                .append("用户名: ").append(TgHtml.escape(ctx.getTgUsername())).append("\n");
        TgAccountEntity account = ctx.getAccount();
        // ctx.account 由 webhook 层按 bot_id + tg_user_id 查询，存在即说明当前用户已绑定。
        if (account == null) {
            sb.append("账号绑定: ").append(TgHtml.bold("未绑定"));
        } else {
            sb.append("账号绑定: ").append(TgHtml.bold("已绑定")).append("\n")
                    .append("租户ID: ").append(TgHtml.code(account.getTenantId())).append("\n")
                    .append("用户ID: ").append(TgHtml.code(account.getUserId())).append("\n")
                    .append("主体ID: ").append(TgHtml.code(account.getSubjectId()));
        }
        TgChatEntity chat = ctx.getChat();
        // 群聊中还可以展示当前群的绑定主体，方便排查群权限问题。
        if (chat != null) {
            sb.append("\n\n").append("群绑定: ").append(TgHtml.bold("已绑定")).append("\n")
                    .append("租户ID: ").append(TgHtml.code(chat.getTenantId())).append("\n")
                    .append("商户ID: ").append(TgHtml.code(chat.getMerchantId()));
        }
        return sb.toString();
    }
}
