package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgBotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Telegram 机器人配置 DAO。
 * <p>用于维护 bot token 密文、Webhook 地址、启停状态等机器人基础配置。</p>
 */
@Mapper
public interface TgBotDao extends BaseDao<TgBotEntity> {
}
