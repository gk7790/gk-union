package com.gk.devtools.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.devtools.entity.DataSourceEntity;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理
 *
 * @author Lowen
 */
public interface DataSourceService extends BaseService<DataSourceEntity> {

    PageData<DataSourceEntity> page(Map<String, Object> params);

    List<DataSourceEntity> list();
}