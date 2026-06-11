package com.gk.telegram.command.handler;

import com.gk.telegram.command.TgCommandContext;
import com.gk.telegram.command.TgCommandHandler;
import com.gk.telegram.entity.TgBindCodeEntity;
import com.gk.telegram.service.TgAccountService;
import com.gk.telegram.service.TgBindCodeService;
import com.gk.telegram.support.TgHtml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * /bind 绑定系统账号: /bind &lt;绑定码&gt;
 */
@Component
@RequiredArgsConstructor
public class BindCommandHandler implements TgCommandHandler {
    private final TgBindCodeService tgBindCodeService;
    private final TgAccountService tgAccountService;

    @Override
    public String command() {
        return "/bind";
    }

    @Override
    public String description() {
        return "绑定系统账号: /bind <绑定码>";
    }

    @Override
    public boolean requireBinding() {
        return false;
    }

    @Override
    public String handle(TgCommandContext ctx) {
        String code = ctx.arg(0);
        if (code == null || code.isBlank()) {
            return "用法: " + TgHtml.code("/bind <绑定码>");
        }
        TgBindCodeEntity bindCode = tgBindCodeService.consume(code.trim(), ctx.getTgUserId());
        if (bindCode == null) {
            return "绑定码无效、已被使用或已过期, 请重新获取。";
        }
        tgAccountService.bind(ctx.getBot(), ctx.getTgUserId(), ctx.getTgUsername(), ctx.getLanguageCode(), bindCode);
        return TgHtml.bold("绑定成功") + "! 现在可以使用 "
                + TgHtml.code("/balance") + "、" + TgHtml.code("/order") + " 等指令。";
    }
}
