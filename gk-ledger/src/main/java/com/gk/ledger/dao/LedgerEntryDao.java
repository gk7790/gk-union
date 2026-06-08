package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerEntryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerEntryDao extends BaseDao<LedgerEntryEntity> {
}
