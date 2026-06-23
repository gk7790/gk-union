package com.gk.meta.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.entity.SysCurrencyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysCurrencyDao extends BaseDao<SysCurrencyEntity> {
    List<SysCurrencyDTO> selectOptions(@Param("statusList") List<Integer> statusList,
                                       @Param("tenantId") Long tenantId);
}
