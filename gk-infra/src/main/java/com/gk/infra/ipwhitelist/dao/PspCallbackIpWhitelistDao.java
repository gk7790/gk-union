package com.gk.infra.ipwhitelist.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.ipwhitelist.entity.PspCallbackIpWhitelistEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PspCallbackIpWhitelistDao extends BaseDao<PspCallbackIpWhitelistEntity> {
}
