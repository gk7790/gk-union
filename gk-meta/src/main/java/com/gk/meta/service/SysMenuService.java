package com.gk.meta.service;


import com.gk.common.core.service.BaseService;
import com.gk.common.dto.AuthUser;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.entity.SysMenuEntity;

import java.util.List;


/**
 * 菜单管理
 * 
 * @author Lowen
 */
public interface SysMenuService extends BaseService<SysMenuEntity> {

	SysMenuDTO get(Long id);

	void addMenu(SysMenuEntity dto);

	void update(SysMenuDTO dto);

	void delete(Long id);

	/**
	 * 菜单列表
	 *
	 * @param typeList 菜单类型
	 */
	List<SysMenuDTO> getAllMenuList(List<Integer> typeList);

	/**
	 * 用户菜单列表
	 *
	 * @param user  用户
	 * @param typeList 菜单类型
	 */
	List<SysMenuDTO> getUserMenuList(AuthUser user, List<Integer> typeList);

	/**
	 * 根据父菜单，查询子菜单
	 * @param pid  父菜单ID
	 */
	List<SysMenuDTO> getListPid(Long pid);
}