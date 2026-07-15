package com.gk.iam.service;

import com.gk.common.core.service.BaseService;
import com.gk.iam.entity.SysUserSubjectEntity;

public interface SysUserSubjectService extends BaseService<SysUserSubjectEntity> {
    SysUserSubjectEntity getByUserId(Long userId);

    SysUserSubjectEntity getActiveSubject(String subjectType, Long tenantId, Long businessSubjectId,
                                          Long userSubjectId, Long userId);

    SysUserSubjectEntity saveOrUpdate(Long userId, SysUserSubjectEntity subject);

    void deleteByUserIds(Long[] userIds);
}
