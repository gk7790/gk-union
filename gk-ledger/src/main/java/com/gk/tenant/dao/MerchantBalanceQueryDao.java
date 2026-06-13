package com.gk.tenant.dao;

import com.gk.common.model.DynMap;
import com.gk.tenant.dto.MerchantBalanceDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantBalanceQueryDao {
    Long countMerchantBalances(@Param("params") DynMap params);

    List<MerchantBalanceDTO> pageMerchantBalances(@Param("params") DynMap params);
}
