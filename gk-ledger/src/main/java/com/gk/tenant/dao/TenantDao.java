package com.gk.tenant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.tenant.entity.TenantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantDao extends BaseDao<TenantEntity> {
}
