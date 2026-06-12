package com.gk.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.infra.enums.StatusEnum;
import com.gk.common.model.DynMap;
import com.gk.platform.dto.SysDeptDTO;
import com.gk.platform.dto.SysRoleDTO;
import com.gk.platform.dto.SysUserDTO;
import com.gk.platform.entity.SysRoleEntity;
import com.gk.platform.service.SysDeptService;
import com.gk.platform.service.SysRoleMenuService;
import com.gk.platform.service.SysRoleService;
import com.gk.platform.service.SysUserService;
import com.gk.tenant.dao.SysTenantDao;
import com.gk.tenant.dto.SysTenantDTO;
import com.gk.tenant.dto.TenantOnboardRequest;
import com.gk.tenant.dto.TenantOnboardResult;
import com.gk.tenant.entity.SysTenantEntity;
import com.gk.tenant.service.SysTenantService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends CrudServiceImpl<SysTenantDao, SysTenantEntity, SysTenantDTO> implements SysTenantService {
    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysUserService sysUserService;

    @Override
    public QueryWrapper<SysTenantEntity> getWrapper(DynMap params) {
        QueryWrapper<SysTenantEntity> wrapper = new QueryWrapper<>();
        return wrapper;
    }

    @Override
    public List<LabelDTO> getDict(DynMap params) {
        List<Integer> list = params.getList("status", Integer.class, StatusEnum.defaultStatus());

        QueryWrapper<SysTenantEntity> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name");
        wrapper.in("status", list);
        List<SysTenantEntity> result = baseDao.selectList(wrapper);

        return result.stream().map(item -> new LabelDTO(item.getId(), item.getName())).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantOnboardResult onboard(TenantOnboardRequest request) {
        requirePlatformSubject();
        validateRequest(request);

        SysTenantDTO tenant = request.getTenant();
        if (tenant.getStatus() == null) {
            tenant.setStatus(StatusEnum.NORMAL.code());
        }
        save(tenant);

        SysDeptDTO dept = createDefaultDept(tenant.getId(), request.getDeptName());
        SysRoleDTO role = copyTenantRoleTemplate(request.getRoleTemplateId(), tenant.getId());
        SysUserDTO user = createTenantAdminUser(request.getAdminUser(), tenant.getId(), dept.getId(), role.getId());

        return new TenantOnboardResult(tenant.getId(), dept.getId(), role.getId(), user.getId());
    }

    private void requirePlatformSubject() {
        if (!SubjectTypeEnum.PLATFORM.matches(ReqContextHolder.getSubjectType())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateRequest(TenantOnboardRequest request) {
        if (request == null || request.getTenant() == null || request.getAdminUser() == null || request.getRoleTemplateId() == null) {
            throw new GkException(ErrorCode.NOT_NULL);
        }
        if (StringUtils.isBlank(request.getTenant().getName())) {
            throw new GkException(ErrorCode.NOT_NULL, "tenant.name");
        }
        if (StringUtils.isBlank(request.getTenant().getCode())) {
            throw new GkException(ErrorCode.NOT_NULL, "tenant.code");
        }
        if (StringUtils.isBlank(request.getAdminUser().getUsername())) {
            throw new GkException(ErrorCode.NOT_NULL, "adminUser.username");
        }
        if (StringUtils.isBlank(request.getAdminUser().getPassword())) {
            throw new GkException(ErrorCode.NOT_NULL, "adminUser.password");
        }
    }

    private SysDeptDTO createDefaultDept(Long tenantId, String deptName) {
        SysDeptDTO dept = new SysDeptDTO();
        dept.setTenantId(tenantId);
        dept.setPid(Constant.DEPT_ROOT);
        dept.setName(StringUtils.defaultIfBlank(deptName, "默认部门"));
        dept.setSort(0);
        dept.setStatus(StatusEnum.NORMAL.code());
        sysDeptService.save(dept);
        return dept;
    }

    private SysRoleDTO copyTenantRoleTemplate(Long roleTemplateId, Long tenantId) {
        SysRoleEntity template = sysRoleService.selectById(roleTemplateId);
        if (template == null
                || !SubjectTypeEnum.TENANT.matches(template.getRoleScope())
                || !isRoleTemplate(template)
                || !StatusEnum.NORMAL.code().equals(template.getStatus())) {
            throw new GkException(ErrorCode.FORBIDDEN);
        }

        SysRoleDTO role = new SysRoleDTO();
        role.setTenantId(tenantId);
        role.setAuth(template.getAuth());
        role.setName(template.getName());
        role.setRemark(template.getRemark());
        role.setRoleScope(SubjectTypeEnum.TENANT.code());
        role.setDataScope(template.getDataScope());
        role.setStatus(template.getStatus() == null ? StatusEnum.NORMAL.code() : template.getStatus());
        role.setMenuIdList(sysRoleMenuService.getMenuIdList(roleTemplateId));
        role.setDeptIdList(Collections.emptyList());
        sysRoleService.save(role);
        return role;
    }

    private SysUserDTO createTenantAdminUser(SysUserDTO adminUser, Long tenantId, Long deptId, Long roleId) {
        adminUser.setId(null);
        adminUser.setTenantId(tenantId);
        adminUser.setMerchantId(null);
        adminUser.setDeptId(deptId);
        adminUser.setSubjectType(SubjectTypeEnum.TENANT.code());
        adminUser.setRoleId(roleId);
        adminUser.setRoleIdList(List.of(roleId));
        if (adminUser.getStatus() == null) {
            adminUser.setStatus(StatusEnum.NORMAL.code());
        }
        sysUserService.save(adminUser);
        return adminUser;
    }

    private boolean isRoleTemplate(SysRoleEntity role) {
        Long tenantId = role.getTenantId();
        return tenantId == null || Constant.DEFAULT_TENANT_ID.equals(tenantId);
    }
}