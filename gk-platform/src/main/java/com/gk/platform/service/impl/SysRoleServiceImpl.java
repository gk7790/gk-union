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
import com.gk.platform.service.SysRoleDataScopeService;
import com.gk.platform.service.SysRoleMenuService;
import com.gk.platform.service.SysRoleService;
import com.gk.platform.service.SysRoleUserService;
import com.gk.meta.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 角色管理服务。
 * <p>
 * 角色按 {@code tenant_id} 分层：
 * <ul>
 *     <li>{@code tenant_id = 0}：系统模板，仅供复制参考，不可直接分配给用户</li>
 *     <li>{@code tenant_id = 1}：平台机构专用角色，仅 PLATFORM 主体可用</li>
 *     <li>{@code tenant_id >= 2}：租户/商户实例角色，可分配给用户</li>
 * </ul>
 * {@code role_scope}（PLATFORM/TENANT/MERCHANT）决定角色适用哪类主体，并与菜单 {@code subject_types} 对齐。
 * 非超管的可见/可改范围由当前登录租户与主体类型约束。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends BaseServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {
	private final SysRoleMenuService sysRoleMenuService;
	private final SysRoleDataScopeService sysRoleDataScopeService;
	private final SysRoleUserService sysRoleUserService;
	private final SysMenuService sysMenuService;

	/** 角色分页。 */
    @Override
	public PageData<SysRoleDTO> page(DynMap params) {
		IPage<SysRoleEntity> page = baseDao.selectPage(
			getPage(params, Constant.CREATED_AT, false),
			getWrapper(params)
		);

		PageData<SysRoleDTO> pageData = getPageData(page, SysRoleDTO.class);
		return pageData;
	}

	/** 角色列表，过滤规则与分页一致。 */
	@Override
	public List<SysRoleDTO> list(DynMap params) {
		List<SysRoleEntity> entityList = baseDao.selectList(getWrapper(params));
		List<SysRoleDTO> dtoList = ConvertUtils.sourceToTarget(entityList, SysRoleDTO.class);
		return dtoList;
	}

	/**
	 * 构建角色查询条件。
	 * <p>
	 * 支持按名称、主体范围、租户、状态过滤；查模板角色可直接传 {@code tenantId=0}。
	 * 所有列表默认隐藏 {@code id < MIN_SYS_ID} 的内置角色；非超管额外做租户隔离。
	 */
	private QueryWrapper<SysRoleEntity> getWrapper(DynMap params) {
		String name = params.getStr("name");
		String roleScope = params.getStr("roleScope");
		Long tenantId = params.getLong("tenantId", null);
		List<Integer> statusList = params.getList("status", Integer.class, null);

		QueryWrapper<SysRoleEntity> wrapper = new QueryWrapper<>();
		wrapper.like(StringUtils.isNotBlank(name), "name", name);
		wrapper.eq(StringUtils.isNotBlank(roleScope), "role_scope", roleScope);
		wrapper.eq(tenantId != null, "tenant_id", tenantId);
		wrapper.in(statusList != null && !statusList.isEmpty(), "status", statusList);

        wrapper.ge("id", Constant.MIN_SYS_ID);
        if (!ReqContextHolder.isSuperAdmin()) {
			applyNonAdminTenantScope(wrapper);
		}

		return wrapper;
	}

	/**
	 * 非超管租户隔离：本租户角色 + 系统预置模板（tenant_id=0 且 dept_id=0）；
	 * 非平台用户不可见 tenant_id=1 的平台机构角色。
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

	/** 单条查询；非超管须通过可见性校验。 */
	@Override
	public SysRoleDTO get(Long id) {
		AssertUtils.isNull(id, "id");
		SysRoleEntity entity = baseDao.selectById(id);
		if (entity == null) {
			return null;
		}
        // 非超管只能访问本租户角色、系统模板，或（平台用户）平台机构角色
		assertRoleVisible(entity);
		SysRoleDTO dto = ConvertUtils.sourceToTarget(entity, SysRoleDTO.class);
		return dto;
	}

	/**
	 * 角色下拉字典：默认只查正常状态、可分配给用户的角色（排除模板）。
	 * 用于用户管理页绑定角色时的选项列表。
	 */
    @Override
    public List<LabelDTO> getDict(DynMap params) {
        if (!params.containsKey("status")) {
            params.put("status", StatusEnum.defaultStatus());
        }

        String roleScope = params.getStr("roleScope");

        QueryWrapper<SysRoleEntity> wrapper = getWrapper(params);
        wrapper.select("id", "name");
        wrapper.eq(params.isValueNull("role_scope"), "role_scope", roleScope);
        if (!ReqContextHolder.isSuperAdmin()) {
            wrapper.eq("tenant_id", ReqContextHolder.getTenantId());
            wrapper.in("dept_id", ReqContextHolder.getSubDeptIdsWithSelf());
        }

        List<SysRoleEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }

	/**
	 * 新增角色：tenant_id、dept_id 强制取当前登录用户；忽略前端传入值。
	 */
    @Override
	@Transactional(rollbackFor = Exception.class)
	public void save(SysRoleDTO dto) {
		SysRoleEntity entity = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);
		applyCreateOwnership(entity);
        prepareRoleForCreate(entity);
		sysMenuService.assertMenusMatchRoleScope(entity.getRoleScope(), dto.getMenuIdList());

		insert(entity);
		dto.setId(entity.getId());

		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	/**
	 * 更新角色：tenant_id 不可改；dept_id 仅超管可改。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(SysRoleDTO dto) {
		SysRoleEntity existing = baseDao.selectById(dto.getId());
		if (existing == null) {
			throw new GkException(ErrorCode.NOT_FOUND);
		}
		assertRoleVisible(existing);

		SysRoleEntity incoming = ConvertUtils.sourceToTarget(dto, SysRoleEntity.class);
		SysRoleEntity entity = mergeForUpdate(incoming, existing);
		lockOwnershipOnUpdate(entity, existing, incoming);
		prepareRoleForUpdate(entity, existing);
		sysMenuService.assertMenusMatchRoleScope(entity.getRoleScope(), dto.getMenuIdList());

		updateById(entity);
		sysRoleMenuService.saveOrUpdate(entity.getId(), dto.getMenuIdList());
		sysRoleDataScopeService.saveOrUpdate(entity.getId(), dto.getDeptIdList());
	}

	/**
	 * 批量删除角色，并级联清理用户绑定、菜单权限、数据权限。
	 * 每条角色删除前均校验可见性。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Long[] ids) {
		for (Long id : ids) {
			SysRoleEntity entity = baseDao.selectById(id);
			if (entity == null) {
				throw new GkException(ErrorCode.NOT_FOUND);
			}
			assertRoleVisible(entity);
		}

		baseDao.deleteBatchIds(Arrays.asList(ids));
		sysRoleUserService.deleteByRoleIds(ids);
		sysRoleMenuService.deleteByRoleIds(ids);
		sysRoleDataScopeService.deleteByRoleIds(ids);
	}

	/**
	 * 校验角色是否可分配给指定主体（用户绑角色时调用）。
	 * <ul>
	 *     <li>模板角色（tenant_id=0）不可分配</li>
	 *     <li>平台角色（tenant_id=1）只能绑 PLATFORM 主体</li>
	 *     <li>非超管不可分配 sadmin/admin 标识的保留角色</li>
	 *     <li>租户/商户角色的 tenant_id、role_scope 须与目标主体一致</li>
	 * </ul>
	 */
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
		if (!ReqContextHolder.isSuperAdmin() && isReservedAuth(role.getAuth())) {
			throw new GkException(ErrorCode.ROLE_AUTH_RESERVED);
		}
		if (SubjectTypeEnum.TENANT.matches(subjectType)) {
			if (tenantId == null || !tenantId.equals(roleTenantId)) {
				throw new GkException(ErrorCode.ROLE_SUBJECT_MISMATCH);
			}
			if (!SubjectTypeEnum.TENANT.matches(role.getRoleScope())) {
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

	/**
	 * 创建前：补默认值、按调用方限定租户、校验保留标识与 tenant/scope 组合。
	 */
	private void prepareRoleForCreate(SysRoleEntity entity) {
		normalizeRole(entity);
		assertReservedAuthAllowed(entity.getAuth());
		assertRoleTenantScope(entity);
	}

	/** 更新前校验保留标识与 tenant/scope 组合；归属字段已在 lockOwnershipOnUpdate 中锁定。 */
	private void prepareRoleForUpdate(SysRoleEntity entity, SysRoleEntity existing) {
		normalizeRole(entity);
		assertReservedAuthAllowed(entity.getAuth());
		assertRoleTenantScope(entity);
	}

	/** 部分更新：null 字段保留库内原值；tenant_id / dept_id 由 lockOwnershipOnUpdate 处理。 */
	private SysRoleEntity mergeForUpdate(SysRoleEntity incoming, SysRoleEntity existing) {
		SysRoleEntity merged = new SysRoleEntity();
		merged.setId(existing.getId());
		merged.setAuth(incoming.getAuth() != null ? incoming.getAuth() : existing.getAuth());
		merged.setName(incoming.getName() != null ? incoming.getName() : existing.getName());
		merged.setRemark(incoming.getRemark() != null ? incoming.getRemark() : existing.getRemark());
		merged.setRoleScope(incoming.getRoleScope() != null ? incoming.getRoleScope() : existing.getRoleScope());
		merged.setDataScope(incoming.getDataScope() != null ? incoming.getDataScope() : existing.getDataScope());
		merged.setStatus(incoming.getStatus() != null ? incoming.getStatus() : existing.getStatus());
		return merged;
	}

	/** 新建角色：tenant_id、dept_id 取当前登录用户，忽略请求体。 */
	private void applyCreateOwnership(SysRoleEntity entity) {
		entity.setTenantId(ReqContextHolder.getTenantId());
		entity.setDeptId(ReqContextHolder.getDeptId());
	}

	/** 更新角色：tenant_id 始终不变；dept_id 仅超管可改。 */
	private void lockOwnershipOnUpdate(SysRoleEntity entity, SysRoleEntity existing, SysRoleEntity incoming) {
		entity.setTenantId(existing.getTenantId());
		if (ReqContextHolder.isSuperAdmin()) {
			entity.setDeptId(incoming.getDeptId() != null ? incoming.getDeptId() : existing.getDeptId());
		} else {
			entity.setDeptId(existing.getDeptId());
		}
	}

	/** 空 tenant_id / dept_id 归一为系统默认值（0）。 */
	private void normalizeRole(SysRoleEntity entity) {
		if (entity.getTenantId() == null) {
			entity.setTenantId(Constant.DEFAULT_TENANT_ID);
		}
		if (entity.getDeptId() == null) {
			entity.setDeptId(Constant.DEPT_ROOT);
		}
	}

	/** 可见即可改：非超管只能访问本租户角色、系统模板，或（平台用户）平台机构角色。 */
	private void assertRoleVisible(SysRoleEntity entity) {
		if (ReqContextHolder.isSuperAdmin()) {
			return;
		}
		Long id = entity.getId();
		if (id == null || id < Constant.MIN_SYS_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
		if (!isRoleInCallerScope(entity)) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

	/** 非超管禁止创建/修改为 sadmin、admin 保留角色标识。 */
	private void assertReservedAuthAllowed(String auth) {
		if (ReqContextHolder.isSuperAdmin() || !isReservedAuth(auth)) {
			return;
		}
		throw new GkException(ErrorCode.ROLE_AUTH_RESERVED);
	}

	/**
	 * 校验 tenant_id 与 role_scope 组合合法：
	 * tenant_id=1 必须是 PLATFORM scope；PLATFORM scope 不能挂在普通租户上；
	 * 非平台/非超管不能操作系统级（tenant_id <= 1）角色。
	 */
	private void assertRoleTenantScope(SysRoleEntity entity) {
		Long tenantId = entity.getTenantId();
		if (!ReqContextHolder.isSuperAdmin() && !ReqContextHolder.isPlatform()
				&& tenantId <= Constant.PLATFORM_TENANT_ID) {
			throw new GkException(ErrorCode.FORBIDDEN);
		}
	}

	/** 判断角色是否落在当前调用者的可见租户范围内。 */
	private boolean isRoleInCallerScope(SysRoleEntity role) {
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

	/** 系统保留角色标识：sadmin、admin。 */
	private boolean isReservedAuth(String auth) {
		return auth != null
				&& (Constant.ROLE_AUTH_SADMIN.equalsIgnoreCase(auth)
				|| Constant.ROLE_AUTH_ADMIN.equalsIgnoreCase(auth));
	}

}
