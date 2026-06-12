package com.gk.platform.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.platform.entity.SysRoleUserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户主体与角色关系。
 */
@Mapper
public interface SysRoleUserDao extends BaseDao<SysRoleUserEntity> {
    void deleteByRoleIds(Long[] roleIds);

    void deleteByUserIds(Long[] userIds);

    void deleteByUserSubjectIds(Long[] userSubjectIds);

    List<Long> getRoleIdListBySubjectId(Long userSubjectId);
}
