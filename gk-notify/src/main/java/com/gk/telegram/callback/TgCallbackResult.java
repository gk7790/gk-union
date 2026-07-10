package com.gk.telegram.callback;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TgCallbackResult {
    private String text;
    private boolean showAlert;

    public static TgCallbackResult toast(String text) {
        return TgCallbackResult.builder()
                .text(text)
                .showAlert(false)
                .build();
    }
}
