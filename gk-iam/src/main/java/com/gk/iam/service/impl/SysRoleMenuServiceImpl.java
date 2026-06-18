package com.gk.iam.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.iam.dao.SysRoleMenuDao;
import com.gk.iam.entity.SysRoleMenuEntity;
import com.gk.iam.service.SysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * 角色与菜单对应关系
 *
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends BaseServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

    private static final int INSERT_BATCH_SIZE = 500;

    @Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
		deleteByRoleIds(new Long[]{roleId});
		if (CollUtil.isEmpty(menuIdList)) {
			return;
		}
        List<SysRoleMenuEntity> entities = toInsertEntities(roleId, menuIdList);
        for (int i = 0; i < entities.size(); i += INSERT_BATCH_SIZE) {
            baseDao.insertBatch(entities.subList(i, Math.min(i + INSERT_BATCH_SIZE, entities.size())));
        }
	}

	@Override
	public List<Long> getMenuIdList(Long roleId){
		return baseDao.getMenuIdList(roleId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByRoleIds(Long[] roleIds) {
		baseDao.deleteByRoleIds(roleIds);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByMenuId(Long menuId) {
		baseDao.deleteByMenuId(menuId);
	}

    private List<SysRoleMenuEntity> toInsertEntities(Long roleId, List<Long> menuIdList) {
        Long userId = ReqContextHolder.getUserId();
        Instant now = Instant.now();
        List<SysRoleMenuEntity> entities = new ArrayList<>(menuIdList.size());
        for (Long menuId : new LinkedHashSet<>(menuIdList)) {
            SysRoleMenuEntity entity = new SysRoleMenuEntity();
            entity.setId(IdWorker.getId());
            entity.setRoleId(roleId);
            entity.setMenuId(menuId);
            entity.setCreatedBy(userId);
            entity.setCreatedAt(now);
            entities.add(entity);
        }
        return entities;
    }
}
