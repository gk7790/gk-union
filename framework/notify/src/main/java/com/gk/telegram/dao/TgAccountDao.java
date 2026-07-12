package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Telegram 个人账号绑定表 DAO。
 * <p>主要用于按 botId + tgUserId 查询、保存和更新用户绑定状态。</p>
 */
@Mapper
public interface TgAccountDao extends BaseDao<TgAccountEntity> {
}
