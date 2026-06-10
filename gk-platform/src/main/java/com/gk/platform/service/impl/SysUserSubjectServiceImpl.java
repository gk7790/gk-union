package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.platform.dao.SysUserSubjectDao;
import com.gk.platform.entity.SysUserSubjectEntity;
import com.gk.platform.service.SysUserSubjectService;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class SysUserSubjectServiceImpl extends BaseServiceImpl<SysUserSubjectDao, SysUserSubjectEntity> implements SysUserSubjectService {
    @Override
    public SysUserSubjectEntity getByUserId(Long userId) {
        return baseDao.getByUserId(userId);
    }

    @Override
    public void saveOrUpdate(Long userId, SysUserSubjectEntity subject) {
        if (subject == null || subject.getRoleId() == null) {
            return;
        }
        subject.setUserId(userId);
        SysUserSubjectEntity existed = baseDao.selectOne(new QueryWrapper<SysUserSubjectEntity>().eq("user_id", userId).last("limit 1"));
        if (existed == null) {
            if (subject.getStatus() == null) {
                subject.setStatus(1);
            }
            insert(subject);
            return;
        }
        subject.setId(existed.getId());
        updateById(subject);
    }

    @Override
    public void deleteByUserIds(Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return;
        }
        baseDao.delete(new QueryWrapper<SysUserSubjectEntity>().in("user_id", Arrays.asList(userIds)));
    }

    @Override
    public void deleteByRoleIds(Long[] roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            return;
        }
        baseDao.delete(new QueryWrapper<SysUserSubjectEntity>().in("role_id", Arrays.asList(roleIds)));
    }
}
