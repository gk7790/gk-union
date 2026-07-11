package com.gk.ledger.dao;

import com.gk.common.model.DynMap;
import com.gk.ledger.dto.MerchantBalanceDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MerchantBalanceQueryDao {
    Long countMerchantBalances(DynMap params);

    List<MerchantBalanceDTO> pageMerchantBalances(DynMap params);

    List<MerchantBalanceDTO> listMerchantBalances(DynMap params);
}
