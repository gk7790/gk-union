package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import org.springframework.stereotype.Component;

/**
 * /start 欢迎与绑定引导
 */
@Component
public class StartCommandHandler implements TgCommandHandler {

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "开始 / 查看绑定状态";
    }

    @Override
    public boolean requireBinding() {
        return false;
    }

    @Override
    public String handle(TgCommandContext ctx) {
        if (ctx.getAccount() != null) {
            return "您已绑定系统账号 (userId=" + ctx.getAccount().getUserId() + ")。\n"
                    + "发送 /help 查看全部指令。";
        }
        return "欢迎使用本机器人。\n"
                + "请先在系统中获取绑定码, 然后发送: /bind <绑定码> 完成绑定。\n"
                + "发送 /help 查看全部指令。";
    }
}
