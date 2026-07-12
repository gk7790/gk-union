package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgChatEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Telegram 群/会话绑定 DAO。
 * <p>用于按 botId + chatId 维护群绑定、通知订阅和启停状态。</p>
 */
@Mapper
public interface TgChatDao extends BaseDao<TgChatEntity> {
}
