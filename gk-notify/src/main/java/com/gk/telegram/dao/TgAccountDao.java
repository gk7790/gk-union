package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgAccountEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TgAccountDao extends BaseDao<TgAccountEntity> {
}
