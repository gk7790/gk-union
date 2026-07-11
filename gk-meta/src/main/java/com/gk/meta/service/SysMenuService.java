package com.gk.meta.service;


import com.gk.common.core.service.BaseService;
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
	 * 侧边栏导航：超管返回全部菜单；其他用户按 role_menu + subjectType。
	 */
	List<SysMenuDTO> getNavMenuList(List<Integer> typeList);

	/**
	 * 菜单管理列表（超管看全部目录，其他按当前主体类型过滤）。
	 */
	List<SysMenuDTO> getAdminMenuList(List<Integer> typeList);

	/**
	 * 角色授权可选菜单树（按 roleScope / subjectType 过滤目录）。
	 */
	List<SysMenuDTO> getRoleSelectMenuList(String roleScope, List<Integer> typeList);

	/**
	 * 校验角色绑定的菜单均属于该 roleScope 可见范围。
	 */
	void assertMenusMatchRoleScope(String roleScope, List<Long> menuIdList);

	/**
	 * 根据父菜单，查询子菜单
	 */
	List<SysMenuDTO> getListPid(Long pid);

	/**
	 * 校验菜单名称是否存在
	 */
	boolean isExistsName(Long id, String name);
}
