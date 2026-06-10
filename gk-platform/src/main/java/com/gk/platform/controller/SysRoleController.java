package com.gk.platform.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.platform.dto.SysRoleDTO;
import com.gk.platform.service.SysRoleDataScopeService;
import com.gk.platform.service.SysRoleMenuService;
import com.gk.platform.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 * 
 * @author Lowen
 */
@RestController
@RequestMapping("/sys/role")
@Tag(name = "系统-角色管理", description = "维护平台、租户、商户角色，以及角色菜单权限和数据权限")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class SysRoleController {
	private final SysRoleService sysRoleService;
	private final SysRoleMenuService sysRoleMenuService;
	private final SysRoleDataScopeService sysRoleDataScopeService;
    private final CurrentUser currentUser;

	@GetMapping("page")
	@Operation(summary = "角色分页", description = "分页查询角色。支持按 roleScope、tenantId、templateOnly 过滤。权限码：sys:role:page。")
	@Parameters({
		@Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true) ,
		@Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true) ,
		@Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY) ,
		@Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY) ,
		@Parameter(name = "name", description = "角色名", in = ParameterIn.QUERY)
	})
    @PreAuthorize("hasAuthority('sys:role:page')")
	public R<?> page(@Parameter(hidden = true) @RequestMap DynMap params){
		PageData<SysRoleDTO> page = sysRoleService.page(params);
		return R.ok(page);
	}

    @GetMapping("list")
    @Operation(summary = "角色列表", description = "查询角色列表，通常用于角色管理页面。权限码：sys:role:page。")
    @PreAuthorize("hasAuthority('sys:role:page')")
    public R<?> list(){
        List<SysRoleDTO> data = sysRoleService.list(new DynMap());
        return R.ok(data);
    }

	@GetMapping("dict")
	@Operation(summary = "角色字典", description = "查询角色下拉选项。可通过 roleScope、tenantId、templateOnly 过滤。权限码：sys:role:page。")
	@PreAuthorize("hasAuthority('sys:role:page')")
	public R<?> dict(@RequestMap DynMap params){
		List<LabelDTO> data = sysRoleService.getDict(params);
		return R.ok(data);
	}

	@GetMapping("{id}")
	@Operation(summary = "角色详情", description = "查询角色已绑定的菜单权限和部门数据权限。权限码：sys:role:info。")
	@PreAuthorize("hasAuthority('sys:role:info')")
	public R<?> get(@Parameter(description = "角色ID", required = true) @PathVariable("id") Long id){
		SysRoleDTO data = new SysRoleDTO();
		//查询角色对应的菜单
		List<Long> menuIdList = sysRoleMenuService.getMenuIdList(id);
		data.setMenuIdList(menuIdList);
		//查询角色对应的数据权限
		List<Long> deptIdList = sysRoleDataScopeService.getDeptIdList(id);
		data.setDeptIdList(deptIdList);
		return R.ok(data);
	}

    @GetMapping("menu")
    @Operation(summary = "角色菜单权限", description = "查询指定角色已绑定的菜单ID列表。权限码：sys:role:info。")
    @PreAuthorize("hasAuthority('sys:role:info')")
    public R<?> getMenu(@Parameter(description = "角色ID", required = true) @RequestParam Long id){
        List<Long> menuIdList = sysRoleMenuService.getMenuIdList(id);
        return R.ok(menuIdList);
    }

	@PostMapping
	@Operation(summary = "新增角色", description = "创建角色并绑定菜单权限、部门数据权限。平台角色、租户角色、商户角色通过 roleScope 区分。权限码：sys:role:add。")
	@PreAuthorize("hasAuthority('sys:role:add')")
	public R<?> save(@RequestBody SysRoleDTO dto){
        if (!currentUser.hasAllAuth("sys:role:add")) {
            return R.error(ErrorCode.FORBIDDEN);
        }
        boolean allowed = ReqContextHolder.isSAdmin() || currentUser.hasAllRole("admin");
        if (!allowed) {
            dto.setTenantId(ReqContextHolder.getTenantId());
        }
		sysRoleService.save(dto);
		return R.ok();
	}

	@PutMapping("{id}")
	@Operation(summary = "修改角色", description = "修改角色基础信息、菜单权限和数据权限。权限码：sys:role:update。")
	@PreAuthorize("hasAuthority('sys:role:update')")
	public R<?> update(@Parameter(description = "角色ID", required = true) @PathVariable("id") Long id, @RequestBody SysRoleDTO dto){
        if (!currentUser.hasAllAuth("sys:role:update")) {
            return R.error(ErrorCode.FORBIDDEN);
        }

        dto.setId(id);
        boolean allowed = ReqContextHolder.isSAdmin() || currentUser.hasAllRole("admin");
        if (!allowed) {
            dto.setTenantId(null);
        }
		sysRoleService.update(dto);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "删除角色", description = "批量删除角色，并清理角色菜单、数据权限和用户主体角色绑定。权限码：sys:role:delete。")
    @PreAuthorize("hasAuthority('sys:role:delete')")
	public R<?> delete(@RequestParam Long[] ids){
		//效验数据
		AssertUtils.isArrayEmpty(ids, "id");
		sysRoleService.delete(ids);
		return R.ok();
	}
}