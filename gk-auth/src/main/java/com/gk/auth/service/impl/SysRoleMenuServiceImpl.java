package com.gk.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.auth.dao.SysRoleMenuDao;
import com.gk.auth.entity.SysRoleMenuEntity;
import com.gk.auth.service.SysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 角色与菜单对应关系
 * 
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends BaseServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

//    protected SysRoleMenuServiceImpl(SysRoleMenuDao baseDao) {
//        super(baseDao);
//    }

    @Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
		//先删除角色菜单关系
		deleteByRoleIds(new Long[]{roleId});

		//角色没有一个菜单权限的情况
		if(CollUtil.isEmpty(menuIdList)){
			return ;
		}

		//保存角色菜单关系
		for(Long menuId : menuIdList){
			SysRoleMenuEntity sysRoleMenuEntity = new SysRoleMenuEntity();
			sysRoleMenuEntity.setMenuId(menuId);
			sysRoleMenuEntity.setRoleId(roleId);

			//保存
			insert(sysRoleMenuEntity);
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

}