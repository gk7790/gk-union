package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgUpdateLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Telegram 入站 update 日志 DAO。
 * <p>update_id 作为幂等依据，避免 Telegram 重试 webhook 时重复处理同一条消息。</p>
 */
@Mapper
public interface TgUpdateLogDao extends BaseDao<TgUpdateLogEntity> {
}
