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

    SysMenuEntity getById(@Param("id") Long id);

    /**
     * 菜单目录（管理端 / 角色授权树），按主体类型过滤。
     */
    List<SysMenuEntity> getCatalogMenuList(@Param("typeList") List<String> typeList,
                                           @Param("subjectType") String subjectType);

    /**
     * 导航菜单目录，只返回启用状态的菜单。
     */
    List<SysMenuEntity> getNavCatalogMenuList(@Param("typeList") List<String> typeList,
                                              @Param("subjectType") String subjectType,
                                              @Param("normalStatus") Integer normalStatus);

    /**
     * 当前登录上下文下的导航菜单：用户 + 角色 + 主体类型 + 角色菜单。
     */
    List<SysMenuEntity> getNavMenuList(@Param("userSubjectId") Long userSubjectId,
                                       @Param("subjectType") String subjectType,
                                       @Param("typeList") List<String> typeList,
                                       @Param("normalStatus") Integer normalStatus);

    /**
     * 统计不在指定主体类型可见范围内的菜单数量。
     */
    long countMenusNotInSubjectType(@Param("menuIds") List<Long> menuIds,
                                    @Param("subjectType") String subjectType);

    /**
     * 根据父菜单，查询子菜单
     */
    List<SysMenuEntity> getListPid(Long pid);

}
