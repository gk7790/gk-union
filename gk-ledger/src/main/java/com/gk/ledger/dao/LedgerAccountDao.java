package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerAccountDao extends BaseDao<LedgerAccountEntity> {
}
