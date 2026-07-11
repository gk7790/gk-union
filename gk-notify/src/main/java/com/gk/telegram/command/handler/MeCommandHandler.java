package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.support.TgHtml;
import org.springframework.stereotype.Component;

/**
 * /me 指令处理器。
 * <p>展示当前 Telegram 用户和群绑定状态，返回内容只包含商户侧可见信息。</p>
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
     * 输出当前 Telegram 用户绑定状态；不暴露 tenantId、userId、subjectId 等内部范围信息。
     */
    @Override
    public String handle(TgCommandContext ctx) {
        StringBuilder sb = new StringBuilder(TgHtml.bold("Telegram 用户")).append("\n")
                .append("Telegram ID: ").append(TgHtml.code(ctx.getTgUserId())).append("\n")
                .append("用户名: ").append(TgHtml.escape(ctx.getTgUsername())).append("\n");

        if (ctx.getAccount() == null) {
            sb.append("账号绑定: ").append(TgHtml.bold("未绑定"));
        } else {
            sb.append("账号绑定: ").append(TgHtml.bold("已绑定"));
            appendMerchantScope(sb, ctx.targetMerchantId());
        }

        if (ctx.getChat() != null) {
            sb.append("\n\n").append("群绑定: ").append(TgHtml.bold("已绑定"));
            appendMerchantScope(sb, ctx.getChat().getMerchantId());
        }
        return sb.toString();
    }

    /**
     * 商户可见范围只展示商户ID，不展示租户ID或系统主体ID。
     */
    private void appendMerchantScope(StringBuilder sb, Long merchantId) {
        if (merchantId != null) {
            sb.append("\n").append("商户ID: ").append(TgHtml.code(merchantId));
        }
    }
}
