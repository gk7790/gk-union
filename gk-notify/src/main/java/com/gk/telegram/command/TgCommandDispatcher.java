package com.gk.telegram.command;

import com.gk.telegram.support.TgHtml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 入站指令分发器: 收集所有 {@link TgCommandHandler}, 按命令路由, 内置 /help 与未知命令兜底
 */
@Slf4j
@Component
public class TgCommandDispatcher {
    private final Map<String, TgCommandHandler> handlers = new LinkedHashMap<>();

    public TgCommandDispatcher(List<TgCommandHandler> handlerList) {
        for (TgCommandHandler handler : handlerList) {
            handlers.put(handler.command().toLowerCase(), handler);
        }
    }

    /**
     * 分发并返回回复文本
     */
    public String dispatch(TgCommandContext ctx) {
        String command = ctx.getCommand();
        if (command == null || command.isBlank()) {
            return helpText();
        }
        command = command.toLowerCase();
        if ("/help".equals(command)) {
            return helpText();
        }

        TgCommandHandler handler = handlers.get(command);
        if (handler == null) {
            return "未识别的指令: " + TgHtml.code(command) + "\n\n" + helpText();
        }
        if (handler.requireBinding() && ctx.getAccount() == null) {
            return "当前 Telegram 未绑定系统账号, 暂无法使用该指令。请联系管理员处理账号绑定。";
        }
        try {
            return handler.handle(ctx);
        } catch (Exception e) {
            log.error("Telegram command handle error, command={}, tgUserId={}", command, ctx.getTgUserId(), e);
            return "处理指令时发生错误, 请稍后再试。";
        }
    }

    /**
     * 帮助文本
     */
    public String helpText() {
        StringBuilder sb = new StringBuilder(TgHtml.bold("可用指令")).append("\n");
        sb.append(TgHtml.code("/help")).append(" - 查看帮助\n");
        for (TgCommandHandler handler : handlers.values()) {
            if ("/help".equalsIgnoreCase(handler.command())) {
                continue;
            }
            sb.append(TgHtml.code(handler.command()))
                    .append(" - ")
                    .append(TgHtml.escape(handler.description()))
                    .append("\n");
        }
        return sb.toString().trim();
    }
}
