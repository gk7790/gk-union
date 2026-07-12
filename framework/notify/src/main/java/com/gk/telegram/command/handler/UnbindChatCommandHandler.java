package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.service.TgChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * /unbindchat 指令处理器。
 * <p>将当前 Telegram 群/会话绑定置为失效，后续群内查询和通知推送不再使用该绑定。</p>
 */
@Component
@RequiredArgsConstructor
public class UnbindChatCommandHandler implements TgCommandHandler {
    private final TgChatService tgChatService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/unbindchat";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "解绑当前群";
    }

    /**
     * 群解绑入口允许未绑定群调用，以便返回明确提示。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 将当前 tg_chat 标记为解绑/停用。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        if (ctx.getChat() == null) {
            return "当前会话未绑定。";
        }
        // 群解绑采用状态更新，不物理删除历史绑定记录。
        tgChatService.unbind(ctx.getChat().getId());
        return "群解绑成功。";
    }
}
