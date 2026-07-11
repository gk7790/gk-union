package com.gk.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.psp.balance.model.PspBalanceAccount;
import com.gk.psp.callback.model.PspCallbackAccount;
import com.gk.psp.entity.PspAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PspAccountDao extends BaseDao<PspAccountEntity> {
    PspCallbackAccount selectCallbackAccount(@Param("pspAccountNo") String pspAccountNo);

    PspBalanceAccount selectBalanceAccount(@Param("pspAccountId") Long pspAccountId);

    List<PspBalanceAccount> selectBalanceAccounts();
}
