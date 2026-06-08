package com.gk.tenant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.tenant.entity.SysTenantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysTenantDao extends BaseDao<SysTenantEntity> {
}
