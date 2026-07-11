package com.gk.reference.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.reference.dto.SysCurrencyDTO;
import com.gk.reference.entity.SysCurrencyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysCurrencyDao extends BaseDao<SysCurrencyEntity> {
    List<SysCurrencyDTO> selectOptions(@Param("statusList") List<Integer> statusList);
}
