package com.gk.telegram.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.telegram.entity.TgUpdateLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TgUpdateLogDao extends BaseDao<TgUpdateLogEntity> {
}
