package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgChatEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TgChatDao extends BaseDao<TgChatEntity> {
}
