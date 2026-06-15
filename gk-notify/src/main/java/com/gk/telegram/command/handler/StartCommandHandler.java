package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.support.TgHtml;
import org.springframework.stereotype.Component;

/**
 * /start 欢迎与绑定引导
 */
@Component
public class StartCommandHandler implements TgCommandHandler {

    /**
     * 当前处理器绑定的 Telegram 指令。
     */
    @Override
    public String command() {
        return "/start";
    }

    /**
     * /help 中展示的指令说明。
     */
    @Override
    public String description() {
        return "开始 / 查看绑定状态";
    }

    /**
     * 欢迎入口允许任何 Telegram 用户调用。
     */
    @Override
    public boolean requireBinding() {
        return false;
    }

    /**
     * 根据当前用户是否已绑定，返回不同的新手引导文案。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        if (ctx.getAccount() != null) {
            return "您已绑定系统账号 (" + TgHtml.code("userId=" + ctx.getAccount().getUserId()) + ")。\n"
                    + "发送 " + TgHtml.code("/help") + " 查看全部指令。";
        }
        return TgHtml.bold("欢迎使用本机器人") + "\n"
                + "商户通知绑定请在商户详情生成绑定码, 然后发送: " + TgHtml.code("/merchant <绑定码>") + "。\n"
                + "发送 " + TgHtml.code("/help") + " 查看全部指令。";
    }
}
