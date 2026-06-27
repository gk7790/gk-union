package com.gk.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.psp.callback.model.PspCallbackAccount;
import com.gk.psp.entity.PspAccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PspAccountDao extends BaseDao<PspAccountEntity> {
    PspCallbackAccount selectCallbackAccount(@Param("pspAccountNo") String pspAccountNo);
}
