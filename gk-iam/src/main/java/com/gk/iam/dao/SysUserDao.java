package com.gk.iam.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gk.common.core.dao.BaseDao;
import com.gk.iam.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统用户
 * 
 * @author Lowen
 */
@Mapper
public interface SysUserDao extends BaseMapper<SysUserEntity> {

    /**
     * 获取用户列表
     * @param params
     * @return
     */
	List<SysUserEntity> getList(Map<String, Object> params);

    /**
     * 根据用户id获取用户信息
     * @param id 用户id
     * @return 返回用户信息
     */
	SysUserEntity getById(@Param("id")Long id);

    /**
     * 获取会员信息
     * @param username 用互名称
     * @return 返回会员信息
     */
	SysUserEntity getByUsername(@Param("username") String username);

    /**
     * 修改用户密码
     * @param id 用户id
     * @param newPassword 用户密码
     * @return 返回受影响的行数
     */
	int updatePassword(@Param("id") Long id, @Param("newPassword") String newPassword);

	int updateAuthenticator(@Param("id") Long id,
							@Param("authType") Integer authType,
							@Param("authSecret") String authSecret);

	/**
	 * 根据部门ID，查询用户数
	 */
	int getCountByDeptId(Long deptId);

	/**
	 * 根据部门ID,查询用户ID列表
	 */
	List<Long> getUserIdListByDeptId(List<Long> deptIdList);
}
