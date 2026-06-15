package com.gk.infra.telegram;

import lombok.Builder;
import lombok.Data;

/**
 * Telegram 绑定票据创建请求。
 */
@Data
@Builder
public class TgBindTicketCreateRequest {
    private TgBindPurpose purpose;
    private Long tenantId;
    private Long merchantId;
    private Long subjectId;
    private Long userId;
}
