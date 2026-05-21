package com.gk.devtools.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.devtools.entity.TableInfoEntity;

import java.util.Map;

/**
 * 表
 *
 * @author Lowen
 */
public interface TableInfoService extends BaseService<TableInfoEntity> {

    PageData<TableInfoEntity> page(Map<String, Object> params);

    TableInfoEntity getByTableName(String tableName);

    void deleteByTableName(String tableName);

    void deleteBatchIds(Long[] ids);
}