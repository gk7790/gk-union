package com.gk.telegram.callback;

import com.gk.telegram.entity.TgBotEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TgCallbackContext {
    private TgBotEntity bot;
    private String callbackQueryId;
    private String callbackData;
    private Long tgUserId;
    private String tgUsername;
    private String languageCode;
    private Long chatId;
    private String chatType;
    private String chatTitle;
    private Long messageId;
}
