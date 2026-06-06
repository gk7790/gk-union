package com.gk.ledger.account.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.account.entity.LedgerJournalEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LedgerJournalDao extends BaseDao<LedgerJournalEntity> {
}
