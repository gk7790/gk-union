package com.gk.infra.telegram;

import lombok.Data;

/**
 * Telegram 一次性绑定票据。
 */
@Data
public class TgBindTicket {
    private String code;
    private String purpose;
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private Long userId;

    public boolean purposeMatches(TgBindPurpose expected) {
        return expected != null && expected.name().equalsIgnoreCase(purpose);
    }
}
