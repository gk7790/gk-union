package com.gk.platform.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.platform.entity.SysRoleMenuEntity;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色与菜单对应关系
 * 
 * @author Lowen
 */
@Mapper
public interface SysRoleMenuDao extends BaseDao<SysRoleMenuEntity> {

	/**
	 * 根据角色ID，获取菜单ID列表
	 */
	List<Long> getMenuIdList(Long roleId);

	/**
	 * 根据角色id，删除角色菜单关系
	 * @param roleIds 角色ids
	 */
	void deleteByRoleIds(Long[] roleIds);

	/**
	 * 根据菜单id，删除角色菜单关系
	 * @param menuId 菜单id
	 */
	void deleteByMenuId(Long menuId);

	/**
	 * 批量插入角色菜单关系（单条 INSERT 多行 VALUES）。
	 */
	void insertBatch(@Param("list") List<SysRoleMenuEntity> list);
}