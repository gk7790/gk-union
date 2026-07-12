package com.gk.devtools.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.devtools.entity.TableFieldEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 表
 *
 * @author Lowen
 */
@Mapper
public interface TableFieldDao extends BaseDao<TableFieldEntity> {

    List<TableFieldEntity> getByTableName(String tableName);

    void deleteByTableName(String tableName);

    void deleteBatchTableIds(Long[] tableIds);
}