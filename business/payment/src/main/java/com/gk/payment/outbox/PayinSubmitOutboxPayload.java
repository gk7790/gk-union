package com.gk.payment.outbox;

import java.math.BigDecimal;

/**
 * Carries the lightweight fields needed to locate and verify a payin submit event.
 */
public record PayinSubmitOutboxPayload(
        Long tenantId,
        Long merchantId,
        Long merchantAppId,
        String payinOrderNo,
        String merchantOrderNo,
        String currency,
        BigDecimal amount
) {
}
