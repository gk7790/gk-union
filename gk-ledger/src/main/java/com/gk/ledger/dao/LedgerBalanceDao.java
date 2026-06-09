package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerBalanceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.Instant;

@Mapper
public interface LedgerBalanceDao extends BaseDao<LedgerBalanceEntity> {
    int applyEntry(
            @Param("tenantId") Long tenantId,
            @Param("accountId") Long accountId,
            @Param("balanceChange") BigDecimal balanceChange,
            @Param("debitAmount") BigDecimal debitAmount,
            @Param("creditAmount") BigDecimal creditAmount,
            @Param("lastEntryId") Long lastEntryId,
            @Param("lastJournalNo") String lastJournalNo,
            @Param("postedAt") Instant postedAt,
            @Param("allowNegative") Integer allowNegative
    );
}
