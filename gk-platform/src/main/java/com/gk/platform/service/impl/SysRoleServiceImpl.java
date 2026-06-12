package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.platform.dao.SysRoleDao;
import com.gk.platform.dto.SysRoleDTO;
import com.gk.platform.entity.SysRoleEntity;
import com.gk.platform.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 角色
 * 
 * @author Lowen
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {
	private final SysRoleMenuService sysRoleMenuService;
	private final SysRoleDataScopeService sysRoleDataScopeService;
	private final SysUserSubjectService sysUserSubjectService;

    @Override
	public PageData<SysRoleDTO> page(DynMap params) {
		IPage<SysRoleEntity> page = baseDao.selectPage(
			getPage(params, Constant.CREATED_AT, false),
			getWrapper(params)
		);

		PageData<SysRoleDTO> pageData = getPageData(page, SysRoleDTO.class);
		markReadOnly(pageData.getItems());
		return pageData;
	}

	@Override
	public List<SysRoleDTO> list(DynMap params) {
		List<SysRoleEntity> entityList = baseDao.selectList(getWrapper(params));
		List<SysRoleDTO> dtoList = ConvertUtils.sourceToTarget(entityList, SysRoleDTO.class);
		markReadOnly(dtoList);
		return dtoList;
	}

	private QueryWrapper<SysRoleEntity> getWrapper(DynMap params) {
		String name = params.getStr("name");
		String roleScope = params.getStr("roleScope");
		Long tenantId = params.getLong("tenantId", null);
		List<Integer> statusList = params.getList("status", Integer.class, null);
		Boolean templateOnly = params.getBool("templateOnly", false);

		QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
		wrapper.like(StringUtils.isNotBlank(name), "name", name);
		wrapper.eq(StringUtils.isNotBlank(roleScope), "role_scope", roleScope);
		wrapper.eq(tenantId != null, "tenant_id", tenantId);
		wrapper.in(statusList != null && !statusList.isEmpty(), "status", statusList);
		wrapper.isNull(Boolean.TRUE.equals(templateOnly), "tenant_id");

		if (!ReqContextHolder.isSAdmin()) {
            wrapper.ge("id", Constant.MIN_SYS_ID);
			applyNonAdminTenantScope(wrapper);
		}

		return wrapper;
	}

	/**
	 * 非超管：本租户角色 + 系统预置角色（tenant/dept 为空或 0）。
	 */
	private void applyNonAdminTenantScope(QueryWrapper<SysRoleEntity> wrapper) {
		Long currentTenantId = ReqContextHolder.getTenantId();
		wrapper.and(w -> {
			if (currentTenantId != null) {
				w.eq("tenant_id", currentTenantId).or();
			}
			w.nested(sys -> sys
					.and(x -> x.isNull("tenant_id").or().eq("tenant_id", 0))
					.and(x -> x.isNull("dept_id").or().eq("dept_id", 0)));
		});
	}

	@Override
	public SysRoleDTO get(Long id) {
		assertRoleVisible(id);
		SysRoleEntity entity = baseDao.selectById(id);
		if (entity == null) {
			return null;
		}
		SysRoleDTO dto = ConvertUtils.sourceToTarget(entity, SysRoleDTO.class);
		markReadOnly(List.of(dto));
		return dto;
	}

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        if (!params.containsKey("status")) {
            params.put("status", StatusEnum.defaultStatus());
        }

        QueryWrapper<SysRoleEntity> wrapper = getWrapper(params);
        wrapper.select("id", "name");
        List<SysRoleEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }

    @Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysRoleDTO dto) {
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);

		//保存角色
		insert(entity);
		dto.setId(entity.getId());

		//保存角色菜单关系
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());

		//保存角色数据权限关系
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysRoleDTO dto) {
		assertRoleMutable(dto.getId());
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);

		//更新角色
		updateById(entity);

		//更新角色菜单关系
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());

		//更新角色数据权限关系
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long[] ids) {
		for (Long id : ids) {
			assertRoleVisible(id);
			assertRoleMutable(id);
		}

		//删除角色
		baseDao.deleteBatchIds(Arrays.asList(ids));

		//删除用户主体角色关系
		sysUserSubjectService.deleteByRoleIds(ids);

		//删除角色菜单关系
		sysRoleMenuService.deleteByRoleIds(ids);

		//删除角色数据权限关系
		sysRoleDataScopeService.deleteByRoleIds(ids);
	}

	private void markReadOnly(List<SysRoleDTO> roles) {
		if (roles == null || roles.isEmpty() || ReqContextHolder.isSAdmin()) {
			return;
		}
		for (SysRoleDTO role : roles) {
			role.setReadOnly(isReservedRole(role.getId()));
		}
	}

	private boolean isReservedRole(Long id) {
		return id != null && id >= Constant.MIN_SYS_ID && id <= Constant.MAX_RESERVED_ID;
	}

	private void assertRoleVisible(Long id) {
		AssertUtils.isNull(id, "id");
		if (id < Constant.MIN_SYS_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

	private void assertRoleMutable(Long id) {
		assertRoleVisible(id);
		AssertUtils.isReserved(id);
	}

}
