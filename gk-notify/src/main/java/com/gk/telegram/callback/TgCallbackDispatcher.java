package com.gk.telegram.callback;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TgCallbackDispatcher {
    private final Map<String, TgCallbackHandler> handlers = new LinkedHashMap<>();

    public TgCallbackDispatcher(List<TgCallbackHandler> handlerList) {
        for (TgCallbackHandler handler : handlerList) {
            handlers.put(handler.action().toLowerCase(), handler);
        }
    }

    public TgCallbackResult dispatch(TgCallbackContext ctx) {
        String action = resolveAction(ctx.getCallbackData());
        if (StringUtils.isBlank(action)) {
            return TgCallbackResult.toast("Unsupported action");
        }

        TgCallbackHandler handler = handlers.get(action);
        if (handler == null) {
            return TgCallbackResult.toast("Unsupported action");
        }

        try {
            return handler.handle(ctx);
        } catch (Exception e) {
            log.error("Telegram callback handle error, action={}, tgUserId={}", action, ctx.getTgUserId(), e);
            return TgCallbackResult.toast("Action failed, please try again later");
        }
    }

    private String resolveAction(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        String[] parts = data.trim().split(":", 3);
        if (parts.length >= 2) {
            return (parts[0] + ":" + parts[1]).toLowerCase();
        }
        return parts[0].toLowerCase();
    }
}
