package com.gk.platform.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.platform.dao.SysRoleDataScopeDao;
import com.gk.platform.entity.SysRoleDataScopeEntity;
import com.gk.platform.service.SysRoleDataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 角色数据权限
 *
 * @author Lowen
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysRoleDataScopeServiceImpl extends BaseServiceImpl<SysRoleDataScopeDao, SysRoleDataScopeEntity>
        implements SysRoleDataScopeService {

    private static final int INSERT_BATCH_SIZE = 500;

    @Override
    public List<Long> getDeptIdList(Long roleId) {
        return baseDao.getDeptIdList(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(Long roleId, List<Long> deptIdList) {
        deleteByRoleIds(new Long[]{roleId});
        if (CollUtil.isEmpty(deptIdList)) {
            return;
        }
        List<SysRoleDataScopeEntity> entities = toInsertEntities(roleId, deptIdList);
        for (int i = 0; i < entities.size(); i += INSERT_BATCH_SIZE) {
            baseDao.insertBatch(entities.subList(i, Math.min(i + INSERT_BATCH_SIZE, entities.size())));
        }
    }

    @Override
    public void deleteByRoleIds(Long[] roleIds) {
        baseDao.deleteByRoleIds(roleIds);
    }

    private List<SysRoleDataScopeEntity> toInsertEntities(Long roleId, List<Long> deptIdList) {
        Long userId = ReqContextHolder.getUserId();
        Instant now = Instant.now();
        List<SysRoleDataScopeEntity> entities = new ArrayList<>(deptIdList.size());
        for (Long deptId : new LinkedHashSet<>(deptIdList)) {
            if (deptId == null || deptId == 0L) {
                continue;
            }
            SysRoleDataScopeEntity entity = new SysRoleDataScopeEntity();
            entity.setId(IdWorker.getId());
            entity.setRoleId(roleId);
            entity.setDeptId(deptId);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(now);
            entities.add(entity);
        }
        return entities;
    }
}
