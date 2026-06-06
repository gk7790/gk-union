package com.gk.auth.dao;

import com.gk.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper
public interface SecurityDao {

    Optional<SysUser> getUserByUserId(@Param("uId") Long uId);

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByEmail(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * 查询角色权限列表
     */
    Set<String> getRoleAuthList(Long userId);

    /**
     * 查询所有权限列表
     */
    List<String> getPermissionsList(@Param("typeList") List<Integer> typeList);

    /**
     * 查询用户权限列表
     * @param userId  用户ID
     */
    List<String> getUserPermissionsList(@Param("userId") Long userId, @Param("typeList") List<Integer> typeList);

    Set<Long> getDataScopeList(Long userId);

    Set<Long> getSubDeptIdList(Long deptId);
}