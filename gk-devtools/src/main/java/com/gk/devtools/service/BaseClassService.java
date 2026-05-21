package com.gk.devtools.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.devtools.entity.BaseClassEntity;

import java.util.List;
import java.util.Map;

/**
 * 基类管理
 *
 * @author Lowen
 */
public interface BaseClassService extends BaseService<BaseClassEntity> {

    PageData<BaseClassEntity> page(Map<String, Object> params);

    List<BaseClassEntity> list();
}