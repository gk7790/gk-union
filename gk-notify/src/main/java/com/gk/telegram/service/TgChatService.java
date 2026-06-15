package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.merchant.entity.MerchantEntity;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgChatEntity;

/**
 * Telegram会话/群组服务(出站推送目标)
 */
public interface TgChatService extends CrudService<TgChatEntity, TgChatDTO> {
    /**
     * 查询指定机器人下某个 Telegram chat 的有效绑定。
     */
    TgChatEntity getActiveChat(Long botId, Long chatId);

    /**
     * 将当前 Telegram 群绑定到商户，用于群内订单查询和后续消息推送。
     */
    TgChatEntity bindMerchantChat(Long botId, Long chatId, String chatType, String title, String languageCode, MerchantEntity merchant);

    /**
     * 解绑群会话，保留历史记录并将状态置为停用。
     */
    void unbind(Long id);
}
