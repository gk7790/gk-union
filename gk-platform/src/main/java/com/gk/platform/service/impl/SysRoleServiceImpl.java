package com.gk.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SubjectTypeEnum;
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
import com.gk.meta.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
	private final SysMenuService sysMenuService;

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
		Boolean assignableOnly = params.getBool("assignableOnly", false);

		QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
		wrapper.like(StringUtils.isNotBlank(name), "name", name);
		wrapper.eq(StringUtils.isNotBlank(roleScope), "role_scope", roleScope);
		wrapper.eq(tenantId != null, "tenant_id", tenantId);
		wrapper.in(statusList != null && !statusList.isEmpty(), "status", statusList);
		wrapper.eq(Boolean.TRUE.equals(templateOnly), "tenant_id", Constant.DEFAULT_TENANT_ID);

		if (Boolean.TRUE.equals(assignableOnly)) {
			wrapper.ne("tenant_id", Constant.DEFAULT_TENANT_ID);
		}

		if (!ReqContextHolder.isSAdmin()) {
            wrapper.ge("id", Constant.MIN_SYS_ID);
			applyNonAdminTenantScope(wrapper);
		}

		return wrapper;
	}

	/**
	 * 非超管：本租户角色 + 系统预置模板（tenant_id=0 且 dept_id=0）；非平台用户不可见 tenant_id=1。
	 */
	private void applyNonAdminTenantScope(QueryWrapper<SysRoleEntity> wrapper) {
		if (!ReqContextHolder.isPlatform()) {
			wrapper.ne("tenant_id", Constant.PLATFORM_TENANT_ID);
		}
		Long currentTenantId = ReqContextHolder.getTenantId();
		wrapper.and(w -> {
			if (currentTenantId != null) {
				w.eq("tenant_id", currentTenantId).or();
			}
			w.nested(sys -> sys
					.eq("tenant_id", Constant.DEFAULT_TENANT_ID)
					.eq("dept_id", Constant.DEPT_ROOT));
		});
	}

	@Override
	public SysRoleDTO get(Long id) {
		AssertUtils.isNull(id, "id");
		SysRoleEntity entity = baseDao.selectById(id);
		if (entity == null) {
			return null;
		}
		assertRoleVisible(entity);
		SysRoleDTO dto = ConvertUtils.sourceToTarget(entity, SysRoleDTO.class);
		markReadOnly(List.of(dto));
		return dto;
	}

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        if (!params.containsKey("status")) {
            params.put("status", StatusEnum.defaultStatus());
        }
        params.put("assignableOnly", true);

        QueryWrapper<SysRoleEntity> wrapper = getWrapper(params);
        wrapper.select("id", "name");

        List<SysRoleEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }

    @Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysRoleDTO dto) {
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);
		normalizeRoleEntity(entity);
		applyCallerTenantScope(entity, null);
		assertAuthAssignable(entity.getAuth());
		assertTenantScopeOnSave(entity);
		sysMenuService.assertMenusMatchRoleScope(entity.getRoleScope(), dto.getMenuIdList());

		insert(entity);
		dto.setId(entity.getId());

		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysRoleDTO dto) {
		assertRoleMutable(dto.getId());
		SysRoleEntity existing = baseDao.selectById(dto.getId());
		if (existing == null) {
			throw new GkException(ErrorCode.NOT_FOUND);
		}
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);
		normalizeRoleEntity(entity);
		applyCallerTenantScope(entity, existing);
		assertAuthAssignable(entity.getAuth());
		assertTenantScopeOnSave(entity);
		sysMenuService.assertMenusMatchRoleScope(entity.getRoleScope(), dto.getMenuIdList());

		updateById(entity);
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long[] ids) {
		for (Long id : ids) {
			assertRoleVisible(id);
			assertRoleMutable(id);
		}

		baseDao.deleteBatchIds(Arrays.asList(ids));
		sysUserSubjectService.deleteByRoleIds(ids);
		sysRoleMenuService.deleteByRoleIds(ids);
		sysRoleDataScopeService.deleteByRoleIds(ids);
	}

	@Override
	public void assertRoleAssignable(Long roleId, String subjectType, Long tenantId, Long merchantId) {
		AssertUtils.isNull(roleId, "roleId");
		SysRoleEntity role = baseDao.selectById(roleId);
		if (role == null) {
			throw new GkException(ErrorCode.NOT_FOUND);
		}
		Long roleTenantId = role.getTenantId() == null ? Constant.DEFAULT_TENANT_ID : role.getTenantId();

		if (Constant.DEFAULT_TENANT_ID.equals(roleTenantId)) {
			throw new GkException(ErrorCode.ROLE_TEMPLATE_NOT_ASSIGNABLE);
		}
		if (Constant.PLATFORM_TENANT_ID.equals(roleTenantId)
				&& !SubjectTypeEnum.PLATFORM.matches(subjectType)) {
			throw new GkException(ErrorCode.ROLE_PLATFORM_ONLY);
		}
		if (!ReqContextHolder.isSAdmin() && isReservedAuth(role.getAuth())) {
			throw new GkException(ErrorCode.ROLE_AUTH_RESERVED);
		}
		if (SubjectTypeEnum.TENANT.matches(subjectType)) {
			if (tenantId == null || !tenantId.equals(roleTenantId)) {
				throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
			}
			return;
		}
		if (SubjectTypeEnum.MERCHANT.matches(subjectType)) {
			if (tenantId == null || !tenantId.equals(roleTenantId)) {
				throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
			}
			if (!SubjectTypeEnum.MERCHANT.matches(role.getRoleScope())) {
				throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
			}
			return;
		}
		if (SubjectTypeEnum.PLATFORM.matches(subjectType)
				&& !SubjectTypeEnum.PLATFORM.matches(role.getRoleScope())) {
			throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
		}
	}

	private void normalizeRoleEntity(SysRoleEntity entity) {
		if (entity.getTenantId() == null) {
			entity.setTenantId(Constant.DEFAULT_TENANT_ID);
		}
		if (entity.getDeptId() == null) {
			entity.setDeptId(Constant.DEPT_ROOT);
		}
	}

	private void applyCallerTenantScope(SysRoleEntity entity, SysRoleEntity existing) {
		if (ReqContextHolder.isSAdmin() || ReqContextHolder.isPlatform()) {
			return;
		}
		Long currentTenantId = ReqContextHolder.getTenantId();
		AssertUtils.isNull(currentTenantId, "tenantId");
		if (existing != null) {
			entity.setTenantId(existing.getTenantId());
			return;
		}
		entity.setTenantId(currentTenantId);
	}

	private void assertAuthAssignable(String auth) {
		if (ReqContextHolder.isSAdmin() || !isReservedAuth(auth)) {
			return;
		}
		throw new GkException(ErrorCode.ROLE_AUTH_RESERVED);
	}

	private void assertTenantScopeOnSave(SysRoleEntity entity) {
		Long tenantId = entity.getTenantId();
		String roleScope = entity.getRoleScope();

		if (Constant.PLATFORM_TENANT_ID.equals(tenantId)
				&& !SubjectTypeEnum.PLATFORM.matches(roleScope)) {
			throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
		}
		if (SubjectTypeEnum.PLATFORM.matches(roleScope)
				&& tenantId > Constant.PLATFORM_TENANT_ID) {
			throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
		}
		if (!ReqContextHolder.isSAdmin() && !ReqContextHolder.isPlatform()
				&& tenantId <= Constant.PLATFORM_TENANT_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

	private boolean isReservedAuth(String auth) {
		return Constant.ROLE_AUTH_SADMIN.equalsIgnoreCase(auth)
				|| Constant.ROLE_AUTH_ADMIN.equalsIgnoreCase(auth);
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
		SysRoleEntity entity = baseDao.selectById(id);
		if (entity == null) {
			throw new GkException(ErrorCode.NOT_FOUND);
		}
		assertRoleVisible(entity);
	}

	/**
	 * 超管可访问任意角色；非超管仅可访问 id≥MIN_SYS_ID 且在本租户/系统预置范围内的角色。
	 */
	private void assertRoleVisible(SysRoleEntity entity) {
		if (ReqContextHolder.isSAdmin()) {
			return;
		}
		Long id = entity.getId();
		if (id == null || id < Constant.MIN_SYS_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
		if (!isRoleInTenantScope(entity)) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

	private boolean isRoleInTenantScope(SysRoleEntity role) {
		Long roleTenantId = role.getTenantId() == null ? Constant.DEFAULT_TENANT_ID : role.getTenantId();
		if (Constant.PLATFORM_TENANT_ID.equals(roleTenantId) && !ReqContextHolder.isPlatform()) {
			return false;
		}
		Long currentTenantId = ReqContextHolder.getTenantId();
		if (currentTenantId != null && currentTenantId.equals(roleTenantId)) {
			return true;
		}
		return Constant.DEFAULT_TENANT_ID.equals(roleTenantId)
				&& (role.getDeptId() == null || Objects.equals(role.getDeptId(), Constant.DEPT_ROOT));
	}

	/**
	 * 超管可修改任意角色；非超管不可修改 id ≤ MAX_RESERVED_ID 的系统预置角色。
	 */
	private void assertRoleMutable(Long id) {
		assertRoleVisible(id);
		if (!ReqContextHolder.isSAdmin() && id != null && id <= Constant.MAX_RESERVED_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

}
