package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgMessageTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Telegram 出站消息任务 DAO。
 * <p>用于后续异步发送、重试、死信和幂等控制。</p>
 */
@Mapper
public interface TgMessageTaskDao extends BaseDao<TgMessageTaskEntity> {
}
