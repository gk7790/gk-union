package com.gk.telegram.callback.handler;

import com.gk.telegram.callback.TgCallbackContext;
import com.gk.telegram.callback.TgCallbackHandler;
import com.gk.telegram.callback.TgCallbackResult;
import org.springframework.stereotype.Component;

@Component
public class NoopCallbackHandler implements TgCallbackHandler {

    @Override
    public String action() {
        return "noop";
    }

    @Override
    public TgCallbackResult handle(TgCallbackContext ctx) {
        return TgCallbackResult.toast("已收到");
    }
}
