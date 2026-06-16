package com.gk.platform.service;

import com.gk.common.core.service.BaseService;
import com.gk.platform.entity.SysUserSubjectEntity;

public interface SysUserSubjectService extends BaseService<SysUserSubjectEntity> {
    SysUserSubjectEntity getByUserId(Long userId);

    SysUserSubjectEntity getActiveSubject(String subjectType, Long tenantId, Long merchantId,
                                          Long subjectId, Long userId);

    SysUserSubjectEntity saveOrUpdate(Long userId, SysUserSubjectEntity subject);

    void deleteByUserIds(Long[] userIds);
}
