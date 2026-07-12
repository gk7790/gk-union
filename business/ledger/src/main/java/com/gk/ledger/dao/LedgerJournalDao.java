package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerJournalEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerJournalDao extends BaseDao<LedgerJournalEntity> {
}
