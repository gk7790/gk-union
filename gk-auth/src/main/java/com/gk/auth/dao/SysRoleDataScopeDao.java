package com.gk.auth.dao;

import com.gk.auth.entity.SysRoleDataScopeEntity;
import com.gk.common.core.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色数据权限
 *
 * @author Lowen
 * @since 1.0.0
 */
@Mapper
public interface SysRoleDataScopeDao extends BaseDao<SysRoleDataScopeEntity> {

    /**
     * 根据角色ID，获取部门ID列表
     */
    List<Long> getDeptIdList(Long roleId);

    /**
     * 根据角色id，删除角色数据权限关系
     * @param roleIds 角色ids
     */
    void deleteByRoleIds(Long[] roleIds);
}