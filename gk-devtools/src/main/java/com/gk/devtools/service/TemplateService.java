package com.gk.devtools.service;

import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.devtools.entity.TemplateEntity;

import java.util.List;
import java.util.Map;

/**
 * 模板管理
 *
 * @author Lowen
 */
public interface TemplateService extends BaseService<TemplateEntity> {

    PageData<TemplateEntity> page(Map<String, Object> params);

    List<TemplateEntity> list();

    void updateStatusBatch(Long[] ids, int status);

}