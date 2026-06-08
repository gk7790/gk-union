package com.gk.merchant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.merchant.entity.MerchantEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantDao extends BaseDao<MerchantEntity> {
}
