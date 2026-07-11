package com.gk.telegram.bind;

import lombok.Data;

/** One-time Telegram binding ticket. */
@Data
public class TgBindTicket {
    private String code;
    private String purpose;
    private String subjectType;
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private Long userId;

    public boolean purposeMatches(TgBindPurpose expected) {
        return expected != null && expected.name().equalsIgnoreCase(purpose);
    }
}
