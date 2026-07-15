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
    Set<String> getRoleAuthList(@Param("userSubjectId") Long userSubjectId);

    List<Long> getRoleIdList(@Param("userSubjectId") Long userSubjectId);

    /**
     * 查询所有权限列表
     */
    List<String> getPermissionsList(@Param("typeList") List<String> typeList);

    /**
     * 查询用户权限列表
     */
    List<String> getUserPermissionsList(@Param("userSubjectId") Long userSubjectId, @Param("typeList") List<String> typeList);

    Set<Long> getDataScopeList(@Param("userSubjectId") Long userSubjectId);

    Set<Long> getSubDeptIdList(Long deptId);
}
