package com.gk.infra.ipwhitelist.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.ipwhitelist.entity.SysLoginIpWhitelistEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLoginIpWhitelistDao extends BaseDao<SysLoginIpWhitelistEntity> {
}
