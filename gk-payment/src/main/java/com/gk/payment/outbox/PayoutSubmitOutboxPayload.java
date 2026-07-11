package com.gk.payment.outbox;

import java.math.BigDecimal;

/**
 * 代付提交 outbox 的消息体 * <p>
 * 这里只保存定位和校验所需的轻量字段；真正提交 PSP 时会重新读取 {@code payout_order}
 * 当前记录，并使用订单上已经固化的路由、金额、收款人和冻结状态 */
public record PayoutSubmitOutboxPayload(
        Long tenantId,
        Long merchantId,
        Long merchantAppId,
        String payoutOrderNo,
        String merchantOrderNo,
        String currency,
        BigDecimal amount,
        BigDecimal totalDebitAmount
) {
}
