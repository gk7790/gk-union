package com.gk.infra.ipwhitelist.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.ipwhitelist.entity.MerchantApiIpWhitelistEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantApiIpWhitelistDao extends BaseDao<MerchantApiIpWhitelistEntity> {
}
