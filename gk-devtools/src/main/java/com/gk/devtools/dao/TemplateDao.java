package com.gk.devtools.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.devtools.entity.TemplateEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 模板管理
 *
 * @author Lowen
 */
@Mapper
public interface TemplateDao extends BaseDao<TemplateEntity> {

    int updateStatusBatch(Map<String, Object> map);
}