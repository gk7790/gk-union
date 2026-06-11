package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgMessageTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TgMessageTaskDao extends BaseDao<TgMessageTaskEntity> {
}
