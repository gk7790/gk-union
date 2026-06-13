package com.gk.tenant.query.dao;

import com.gk.common.model.DynMap;
import com.gk.tenant.query.dto.TenantMerchantBalanceDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TenantMerchantBalanceQueryDao {
    Long countTenantMerchantBalances(@Param("params") DynMap params);

    List<TenantMerchantBalanceDTO> pageTenantMerchantBalances(@Param("params") DynMap params);
}
