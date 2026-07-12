package com.gk.devtools.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.devtools.entity.TableInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 列
 *
 * @author Lowen
 */
@Mapper
public interface TableInfoDao extends BaseDao<TableInfoEntity> {

    TableInfoEntity getByTableName(@Param("tableName") String tableName);

    void deleteByTableName(@Param("tableName") String tableName);
}