package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LedgerAccountDao extends BaseDao<LedgerAccountEntity> {
    LedgerAccountEntity selectByIdForUpdate(@Param("id") Long id);
}
