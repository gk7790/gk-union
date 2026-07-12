package com.gk.devtools.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.devtools.entity.DataSourceEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源管理
 *
 * @author Lowen
 */
@Mapper
public interface DataSourceDao extends BaseDao<DataSourceEntity> {
	
}