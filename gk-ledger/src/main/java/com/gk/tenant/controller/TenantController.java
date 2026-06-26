package com.gk.tenant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.iam.dto.SysDeptDTO;
import com.gk.iam.dto.SysRoleDTO;
import com.gk.iam.service.SysDeptService;
import com.gk.iam.service.SysRoleService;
import com.gk.tenant.dto.TenantDTO;
import com.gk.tenant.dto.TenantOnboardRequest;
import com.gk.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统-租户管理", description = "平台用于维护租户资料、租户开户、租户部门和租户角色选择的后台接")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/sys/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;

    @GetMapping("page")
    @Operation(summary = "租户分页", description = "分页查询系统租户。权限码：sys:tenant:page。通常仅平台管理人员使用")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY),
    })
    @PreAuthorize("hasAuthority('sys:tenant:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<TenantDTO> page = tenantService.page(params);
        return R.ok(page);
    }

    @GetMapping("dict")
    @Operation(summary = "租户字典", description = "查询可用租户下拉选项，返回租户ID和租户名称。权限码：sys:tenant:view")
    @PreAuthorize("hasAuthority('sys:tenant:view')")
    public R<?> dict(@RequestMap DynMap params) {
        List<LabelDTO> list = tenantService.getDict(params);
        return R.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增租户", description = "只创建租户基础资料，不会自动创建租户管理员、部门或角色。完整开户请使用“租户开户”接口。权限码：sys:tenant:add")
    @PreAuthorize("hasAuthority('sys:tenant:add')")
    public R<?> save(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "租户基础资料") @RequestBody TenantDTO dto) {
        tenantService.save(dto);
        return R.ok();
    }

    @PostMapping("onboard")
    @Operation(summary = "租户开", description = "平台一次性创建租户、默认部门、复制租户角色模板，并创建租户管理员账号及主体绑定。权限码：sys:tenant:add")
    @PreAuthorize("hasAuthority('sys:tenant:add')")
    public R<?> onboard(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "租户开户请求，包括租户资料、管理员账号、默认部门名称和租户角色模板ID") @RequestBody TenantOnboardRequest request) {
        return R.ok(tenantService.onboard(request));
    }

    @GetMapping("{tenantId}/depts")
    @Operation(summary = "租户部门列表", description = "查询指定租户下的部门树，用于平台给租户用户分配部门。权限码：sys:tenant:view")
    @PreAuthorize("hasAuthority('sys:tenant:view')")
    public R<?> depts(@Parameter(description = "租户ID", required = true) @PathVariable Long tenantId) {
        DynMap params = new DynMap();
        params.put("tenantId", tenantId);
        List<SysDeptDTO> list = sysDeptService.list(params);
        return R.ok(list);
    }

    @GetMapping("{tenantId}/roles")
    @Operation(summary = "租户角色列表", description = "查询指定租户可分配给租户用户的角色，只返role_scope=TENANT 且属于该租户的启用角色。权限码：sys:tenant:view")
    @PreAuthorize("hasAuthority('sys:tenant:view')")
    public R<?> roles(@Parameter(description = "租户ID", required = true) @PathVariable Long tenantId) {
        DynMap params = new DynMap();
        params.put("tenantId", tenantId);
        params.put("roleScope", SubjectTypeEnum.TENANT.code());
        params.put("status", List.of(StatusEnum.NORMAL.code()));
        List<SysRoleDTO> list = sysRoleService.list(params);
        return R.ok(list);
    }

    @GetMapping("role-templates")
    @Operation(summary = "租户角色模板列表", description = "查询平台预置的租户角色模板，用于租户开户时复制出该租户自己的角色。权限码：sys:tenant:view")
    @PreAuthorize("hasAuthority('sys:tenant:view')")
    public R<?> roleTemplates() {
        DynMap params = new DynMap();
        params.put("roleScope", SubjectTypeEnum.TENANT.code());
        params.put("tenantId", Constant.DEFAULT_TENANT_ID);
        params.put("status", List.of(StatusEnum.NORMAL.code()));
        List<SysRoleDTO> list = sysRoleService.list(params);
        return R.ok(list);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改租户", description = "修改租户基础资料。权限码：sys:tenant:update")
    @PreAuthorize("hasAuthority('sys:tenant:update')")
    public R<?> update(@Parameter(description = "租户ID", required = true) @PathVariable("id") Long id,
                       @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "租户基础资料") @RequestBody TenantDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        tenantService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除租户", description = "删除租户资料。权限码：sys:tenant:delete")
    @PreAuthorize("hasAuthority('sys:tenant:delete')")
    public R<?> delete(@Parameter(description = "租户ID", required = true) @RequestParam Long id) {
        AssertUtils.isReserved(id);
        tenantService.delete(id);
        return R.ok();
    }
}
