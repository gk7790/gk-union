package com.gk.telegram.service;

import com.gk.common.core.service.CrudService;
import com.gk.telegram.dto.TgChatDTO;
import com.gk.telegram.entity.TgChatEntity;

/**
 * Telegram会话/群组服务(出站推送目标)
 */
public interface TgChatService extends CrudService<TgChatEntity, TgChatDTO> {
}
