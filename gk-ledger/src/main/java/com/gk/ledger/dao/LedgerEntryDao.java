package com.gk.ledger.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.ledger.entity.LedgerEntryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LedgerEntryDao extends BaseDao<LedgerEntryEntity> {
    int insertBatch(@Param("list") List<LedgerEntryEntity> list);
}
