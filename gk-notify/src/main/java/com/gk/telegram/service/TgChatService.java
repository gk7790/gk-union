package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgChatEntity;

import java.util.List;

/**
 * Telegram会话/群组服务(出站推送目标)
 */
public interface TgChatService extends CrudService<TgChatEntity, TgChatDTO> {
    /**
     * 查询指定机器人下某个 Telegram chat 的有效绑定。
     */
    TgChatEntity getActiveChat(Long botId, Long chatId);

    /**
     * 将当前 Telegram 群绑定到系统主体，支持平台、租户和商户群。
     */
    TgChatEntity bindSubjectChat(Long botId, Long chatId, String chatType, String title, String languageCode,
                                 SysUserSubjectEntity subject);

    /**
     * 批量查询商户当前启用中的群绑定。
     */
    List<TgChatDTO> listActiveMerchantChats(List<Long> merchantIds);

    /**
     * 解绑商户当前启用中的群绑定。
     */
    void unbindMerchantChat(Long merchantId);

    /**
     * 给商户当前绑定的群发送一条后台通知。
     */
    void sendMerchantMessage(Long merchantId, String content, String payloadJson);

    /**
     * 解绑群会话，保留历史记录并将状态置为停用。
     */
    void unbind(Long id);
}
