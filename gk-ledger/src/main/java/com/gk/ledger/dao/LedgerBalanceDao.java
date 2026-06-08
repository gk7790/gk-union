package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerBalanceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerBalanceDao extends BaseDao<LedgerBalanceEntity> {
}
