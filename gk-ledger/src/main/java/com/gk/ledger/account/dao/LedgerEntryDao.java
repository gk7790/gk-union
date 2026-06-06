package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerEntryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerEntryDao extends BaseDao<LedgerEntryEntity> {
}
