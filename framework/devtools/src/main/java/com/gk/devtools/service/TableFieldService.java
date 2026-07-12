package com.gk.devtools.service;


import com.gk.common.core.service.BaseService;
import com.gk.devtools.entity.TableFieldEntity;

import java.util.List;

/**
 * 列
 *
 * @author Lowen
 */
public interface TableFieldService extends BaseService<TableFieldEntity> {

    List<TableFieldEntity> getByTableName(String tableName);

    void deleteByTableName(String tableName);

    void deleteBatchTableIds(Long[] tableIds);
}