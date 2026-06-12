package com.gk.platform.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.redis.RedisKeys;
import com.gk.common.redis.RedisUtils;
import com.gk.platform.dao.SysRoleUserDao;
import com.gk.platform.entity.SysRoleUserEntity;
import com.gk.platform.service.SysRoleUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 用户主体与角色关系。
 */
@Service
@RequiredArgsConstructor
public class SysRoleUserServiceImpl extends BaseServiceImpl<SysRoleUserDao, SysRoleUserEntity> implements SysRoleUserService {
    private final RedisUtils redisUtils;

    @Override
    public void saveOrUpdate(Long userSubjectId, Long userId, List<Long> roleIdList) {
        clearAuthCache(userSubjectId, userId);
        deleteByUserSubjectIds(new Long[]{userSubjectId});
        if (CollUtil.isEmpty(roleIdList)) {
            return;
        }

        for (Long roleId : new LinkedHashSet<>(roleIdList)) {
            if (roleId == null) {
                continue;
            }
            SysRoleUserEntity entity = new SysRoleUserEntity();
            entity.setUserSubjectId(userSubjectId);
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            insert(entity);
        }
    }

    @Override
    public void deleteByRoleIds(Long[] roleIds) {
        baseDao.deleteByRoleIds(roleIds);
    }

    @Override
    public void deleteByUserIds(Long[] userIds) {
        baseDao.deleteByUserIds(userIds);
        if (userIds != null) {
            for (Long userId : userIds) {
                clearAuthCache(null, userId);
            }
        }
    }

    @Override
    public void deleteByUserSubjectIds(Long[] userSubjectIds) {
        baseDao.deleteByUserSubjectIds(userSubjectIds);
        if (userSubjectIds != null) {
            for (Long userSubjectId : userSubjectIds) {
                clearAuthCache(userSubjectId, null);
            }
        }
    }

    @Override
    public List<Long> getRoleIdListBySubjectId(Long userSubjectId) {
        return baseDao.getRoleIdListBySubjectId(userSubjectId);
    }

    private void clearAuthCache(Long userSubjectId, Long userId) {
        List<String> keys = new ArrayList<>();
        if (userId != null) {
            keys.add(RedisKeys.getSysLonginKey(Constant.ADMIN, userId + ""));
        }
        if (userSubjectId != null) {
            keys.add(RedisKeys.getSubjectDataScopeKey(userSubjectId));
        }
        if (!keys.isEmpty()) {
            redisUtils.delete(keys);
        }
    }
}
