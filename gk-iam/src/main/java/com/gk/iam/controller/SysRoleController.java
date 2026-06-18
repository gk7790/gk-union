package com.gk.iam.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.iam.dto.SysRoleDTO;
import com.gk.iam.service.SysRoleDataScopeService;
import com.gk.iam.service.SysRoleMenuService;
import com.gk.iam.service.SysRoleService;
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

	@GetMapping("page")
	@Operation(summary = "角色分页", description = "分页查询角色。支持按 roleScope、tenantId 过滤；查模板传 tenantId=0。权限码：sys:role:page。")
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
	@Operation(summary = "角色字典", description = "查询可分配给用户的角色下拉选项（不含模板角色）。可通过 roleScope、tenantId 过滤。权限码：sys:role:page。")
	@PreAuthorize("hasAuthority('sys:role:page')")
	public R<?> dict(@RequestMap DynMap params){
		List<LabelDTO> data = sysRoleService.getDict(params);
		return R.ok(data);
	}

	@GetMapping("{id}")
	@Operation(summary = "角色详情", description = "查询角色已绑定的菜单权限和部门数据权限。权限码：sys:role:info。")
	@PreAuthorize("hasAuthority('sys:role:info')")
	public R<?> get(@Parameter(description = "角色ID", required = true) @PathVariable("id") Long id){
		SysRoleDTO data = sysRoleService.get(id);
		if (data == null) {
			return R.error(ErrorCode.NOT_FOUND);
		}
		List<Long> menuIdList = sysRoleMenuService.getMenuIdList(id);
		data.setMenuIdList(menuIdList);
		List<Long> deptIdList = sysRoleDataScopeService.getDeptIdList(id);
		data.setDeptIdList(deptIdList);
		return R.ok(data);
	}

    @GetMapping("menu")
    @Operation(summary = "角色菜单权限", description = "查询指定角色已绑定的菜单ID列表。权限码：sys:role:info。")
    @PreAuthorize("hasAuthority('sys:role:info')")
    public R<?> getMenu(@Parameter(description = "角色ID", required = true) @RequestParam Long id){
        if (sysRoleService.get(id) == null) {
            return R.error(ErrorCode.NOT_FOUND);
        }
        List<Long> menuIdList = sysRoleMenuService.getMenuIdList(id);
        return R.ok(menuIdList);
    }

	@PostMapping
	@Operation(summary = "新增角色", description = "创建角色并绑定菜单权限、部门数据权限。平台角色、租户角色、商户角色通过 roleScope 区分。权限码：sys:role:add。")
	@PreAuthorize("hasAuthority('sys:role:add')")
	public R<?> save(@RequestBody SysRoleDTO dto){
		sysRoleService.save(dto);
		return R.ok();
	}

	@PutMapping("{id}")
	@Operation(summary = "修改角色", description = "修改角色基础信息、菜单权限和数据权限。超管可改任意角色；非超管不可改 id≤1000 的系统预置角色。权限码：sys:role:update。")
	@PreAuthorize("hasAuthority('sys:role:update')")
	public R<?> update(@Parameter(description = "角色ID", required = true) @PathVariable("id") Long id, @RequestBody SysRoleDTO dto){
        dto.setId(id);
		sysRoleService.update(dto);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Operation(summary = "删除角色", description = "删除单个角色，并清理角色菜单、数据权限和用户主体角色绑定。权限码：sys:role:delete。")
	@PreAuthorize("hasAuthority('sys:role:delete')")
	public R<?> deleteById(@Parameter(description = "角色ID", required = true) @PathVariable("id") Long id) {
		AssertUtils.isNull(id, "id");
		sysRoleService.delete(new Long[]{id});
		return R.ok();
	}
}
