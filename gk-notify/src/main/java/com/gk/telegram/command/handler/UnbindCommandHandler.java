package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.service.TgAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * /unbind 指令处理器。
 * <p>将当前 Telegram 私聊用户的个人绑定置为失效，保留历史绑定记录。</p>
 */
@Component
@RequiredArgsConstructor
public class UnbindCommandHandler implements TgCommandHandler {
    private final TgAccountService tgAccountService;

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/unbind";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "解绑当前 Telegram 用户";
    }

    /**
     * 解绑入口允许未绑定用户调用，以便返回明确提示。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 将当前 tg_account 标记为解绑。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        if (ctx.getAccount() == null) {
            return "当前 Telegram 用户未绑定。";
        }
        // 账号解绑采用状态更新，不物理删除历史绑定记录。
        tgAccountService.unbind(ctx.getAccount().getId());
        return "账号解绑成功。";
    }
}
