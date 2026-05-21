package com.gk.meta.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.meta.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单管理
 * 
 * @author Lowen
 */
@Mapper
public interface SysMenuDao extends BaseDao<SysMenuEntity> {

	SysMenuEntity getById(@Param("id") Long id, @Param("language") String language);

	/**
	 * 查询所有菜单列表
	 *
	 * @param typeList 菜单类型
	 */
	List<SysMenuEntity> getMenuList(@Param("typeList") List<Integer> typeList, @Param("language") String language);

	/**
	 * 查询用户菜单列表
	 *
	 * @param userId 用户ＩＤ
	 * @param typeList 菜单类型
	 * @param language 语言
	 */
	List<SysMenuEntity> getUserMenuList(@Param("userId") Long userId, @Param("typeList") List<Integer> typeList, @Param("language") String language);


	/**
	 * 根据父菜单，查询子菜单
	 * @param pid  父菜单ID
	 */
	List<SysMenuEntity> getListPid(Long pid);

}