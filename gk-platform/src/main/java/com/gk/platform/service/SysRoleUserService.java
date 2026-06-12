package com.gk.platform.service;

import com.gk.common.core.service.BaseService;
import com.gk.platform.entity.SysRoleUserEntity;

import java.util.List;

/**
 * 用户主体与角色关系。
 */
public interface SysRoleUserService extends BaseService<SysRoleUserEntity> {

    void saveOrUpdate(Long userSubjectId, Long userId, List<Long> roleIdList);

    void deleteByRoleIds(Long[] roleIds);

    void deleteByUserIds(Long[] userIds);

    void deleteByUserSubjectIds(Long[] userSubjectIds);

    List<Long> getRoleIdListBySubjectId(Long userSubjectId);
}
