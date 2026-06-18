package com.gk.iam.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.iam.entity.SysUserSubjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserSubjectDao extends BaseDao<SysUserSubjectEntity> {
    SysUserSubjectEntity getByUserId(@Param("userId") Long userId);
}
