package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.beans.CurrentUser;
import com.gk.common.context.ReqContext;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.AuthUser;
import com.gk.common.enums.AdminEnum;
import com.gk.common.model.PageData;
import com.gk.common.password.PasswordUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.platform.dao.SysUserDao;
import com.gk.platform.dto.SysUserDTO;
import com.gk.platform.entity.SysUserEntity;
import com.gk.platform.service.SysDeptService;
import com.gk.platform.service.SysRoleUserService;
import com.gk.platform.service.SysUserPostService;
import com.gk.platform.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 系统用户
 * 
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {
	private final SysDeptService sysDeptService;
    private final SysUserPostService sysUserPostService;
    private final SysRoleUserService sysRoleUserService;

    @Override
	public PageData<SysUserDTO> page(Map<String, Object> params) {
		//转换成like
		paramsToLike(params, "username");

		//分页
		IPage<SysUserEntity> page = getPage(params, "t1.created_at", false);

        //普通管理员，只能查询所属部门及子部门的数据
        if (!ReqContextHolder.isSAdmin()) {
            params.put("deptIdList", ReqContextHolder.getSubDeptIds());
			params.put("selfId", ReqContextHolder.getUserId());
        }

		//查询
		List<SysUserEntity> list = baseDao.getList(params);

		return getPageData(list, page.getTotal(), SysUserDTO.class);
	}

	@Override
	public List<SysUserDTO> list(Map<String, Object> params) {
		//普通管理员，只能查询子部门的数据
        if (!ReqContextHolder.isSAdmin()) {
            params.put("deptIdList", ReqContextHolder.getSubDeptIds());
            params.put("selfId", ReqContextHolder.getUserId());
        }

		List<SysUserEntity> entityList = baseDao.getList(params);

		return ConvertUtils.sourceToTarget(entityList, SysUserDTO.class);
	}

	@Override
	public SysUserDTO getById(Long id) {
		SysUserEntity entity = baseDao.selectById(id);

		return ConvertUtils.sourceToTarget(entity, SysUserDTO.class);
	}

	@Override
	public SysUserDTO getByUsername(String username) {
		SysUserEntity entity = baseDao.getByUsername(username);
		return ConvertUtils.sourceToTarget(entity, SysUserDTO.class);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysUserDTO dto) {
		SysUserEntity entity = ConvertUtils.sourceToTarget(dto, SysUserEntity.class);

		//密码加密
		String password = PasswordUtils.encode(entity.getPassword());
		entity.setPassword(password);

		//保存用户
		entity.setSuperAdmin(AdminEnum.NO.code());
		insert(entity);

		// 角色用户 保存角色用户关系
		sysRoleUserService.saveOrUpdate(entity.getId(), dto.getRoleIdList());

		//保存用户岗位关系
		sysUserPostService.saveOrUpdate(entity.getId(), dto.getPostIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysUserDTO dto) {
		SysUserEntity entity = ConvertUtils.sourceToTarget(dto, SysUserEntity.class);

		//密码加密
		if(StringUtils.isBlank(dto.getPassword())){
			entity.setPassword(null);
		}else{
			String password = PasswordUtils.encode(entity.getPassword());
			entity.setPassword(password);
		}

		//更新用户
		updateById(entity);

		// 角色用户 更新角色用户关系
		sysRoleUserService.saveOrUpdate(entity.getId(), dto.getRoleIdList());

		//保存用户岗位关系
		sysUserPostService.saveOrUpdate(entity.getId(), dto.getPostIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserInfo(SysUserDTO dto) {
		SysUserEntity entity = selectById(dto.getId());
		entity.setAvatar(dto.getAvatar());
		entity.setRealName(dto.getRealName());
		entity.setGender(dto.getGender());
		entity.setMobile(dto.getMobile());
		entity.setEmail(dto.getEmail());

		updateById(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long[] ids) {
		//删除用户
		baseDao.deleteBatchIds(Arrays.asList(ids));

		// 角色用户 删除角色用户关系
		sysRoleUserService.deleteByUserIds(ids);

		//删除用户岗位关系
		sysUserPostService.deleteByUserIds(ids);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updatePassword(Long id, String newPassword) {
		newPassword = PasswordUtils.encode(newPassword);

		baseDao.updatePassword(id, newPassword);
	}

	@Override
	public int getCountByDeptId(Long deptId) {
		return baseDao.getCountByDeptId(deptId);
	}

	@Override
	public List<Long> getUserIdListByDeptId(List<Long> deptIdList) {
		return baseDao.getUserIdListByDeptId(deptIdList);
	}

}