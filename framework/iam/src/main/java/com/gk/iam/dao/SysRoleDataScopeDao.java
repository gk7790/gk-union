package com.gk.iam.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.iam.entity.SysRoleDataScopeEntity;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

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

    /**
     * 批量插入角色数据权限（单条 INSERT 多行 VALUES）。
     */
    void insertBatch(@Param("list") List<SysRoleDataScopeEntity> list);
}