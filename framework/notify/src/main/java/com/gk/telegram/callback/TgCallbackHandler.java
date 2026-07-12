package com.gk.telegram.callback;

public interface TgCallbackHandler {

    String action();

    default boolean requireBinding() {
        return false;
    }

    TgCallbackResult handle(TgCallbackContext ctx);
}
