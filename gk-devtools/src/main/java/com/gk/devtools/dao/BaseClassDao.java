package com.gk.devtools.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.devtools.entity.BaseClassEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基类管理
 *
 * @author Lowen
 */
@Mapper
public interface BaseClassDao extends BaseDao<BaseClassEntity> {

}