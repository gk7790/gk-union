package com.gk.psp.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.entity.PspBankMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PspBankMappingDao extends BaseDao<PspBankMappingEntity> {

    List<PspBankMappingDTO> selectMatrix(@Param("pspId") Long pspId,
                                         @Param("countryCode") String countryCode,
                                         @Param("currency") String currency);

    int upsert(PspBankMappingEntity entity);
}
