package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgBotEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TgBotDao extends BaseDao<TgBotEntity> {
}
