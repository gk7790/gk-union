package com.gk.infra.telegram;

public interface TgBindTicketService {
    TgBindTicket generate(TgBindPurpose purpose, String subjectType,
                          Long tenantId, Long merchantId, Long subjectId, Long userId);

    TgBindTicket consume(String code, TgBindPurpose expectedPurpose);
}
