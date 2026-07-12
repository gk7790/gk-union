package com.gk.merchant.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.merchant.entity.MerchantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantDao extends BaseDao<MerchantEntity> {
    List<Long> selectMerchantIdsWithActiveTgChat(@Param("merchantIds") List<Long> merchantIds);
}
