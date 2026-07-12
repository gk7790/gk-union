package com.gk.infra.log.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.log.entity.LogErrorEntity;
import org.apache.ibatis.annotations.Mapper;

/**
* 系统异常日志
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-29
*/
@Mapper
public interface LogErrorDao extends BaseDao<LogErrorEntity> {
	
}