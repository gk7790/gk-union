package com.gk.platform.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.platform.entity.SysUserSubjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserSubjectDao extends BaseDao<SysUserSubjectEntity> {
    SysUserSubjectEntity getByUserId(@Param("userId") Long userId);
}
