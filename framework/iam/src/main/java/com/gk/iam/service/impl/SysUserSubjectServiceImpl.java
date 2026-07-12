package com.gk.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.infra.enums.StatusEnum;
import com.gk.iam.dao.SysUserSubjectDao;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysUserSubjectService;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class SysUserSubjectServiceImpl extends BaseServiceImpl<SysUserSubjectDao, SysUserSubjectEntity> implements SysUserSubjectService {
    @Override
    public SysUserSubjectEntity getByUserId(Long userId) {
        return baseDao.getByUserId(userId);
    }

    @Override
    public SysUserSubjectEntity getActiveSubject(String subjectType, Long tenantId, Long merchantId,
                                                 Long subjectId, Long userId) {
        QueryWrapper<SysUserSubjectEntity> wrapper = new QueryWrapper<SysUserSubjectEntity>()
                .eq("subject_type", subjectType)
                .eq("status", StatusEnum.NORMAL.code())
                .eq(subjectId != null, "id", subjectId)
                .eq(userId != null, "user_id", userId)
                .eq(tenantId != null, "tenant_id", tenantId)
                .eq(merchantId != null, "merchant_id", merchantId)
                .last("limit 1");
        return baseDao.selectOne(wrapper);
    }

    @Override
    public SysUserSubjectEntity saveOrUpdate(Long userId, SysUserSubjectEntity subject) {
        if (subject == null) {
            return null;
        }
        subject.setUserId(userId);
        SysUserSubjectEntity existed = baseDao.selectOne(new QueryWrapper<SysUserSubjectEntity>().eq("user_id", userId).last("limit 1"));
        if (existed == null) {
            if (subject.getStatus() == null) {
                subject.setStatus(StatusEnum.NORMAL.code());
            }
            insert(subject);
            return subject;
        }
        subject.setId(existed.getId());
        updateById(subject);
        return subject;
    }

    @Override
    public void deleteByUserIds(Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return;
        }
        baseDao.delete(new QueryWrapper<SysUserSubjectEntity>().in("user_id", Arrays.asList(userIds)));
    }
}
