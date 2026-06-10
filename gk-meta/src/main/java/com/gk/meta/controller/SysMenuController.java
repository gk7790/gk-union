package com.gk.meta.controller;


import cn.hutool.core.util.ObjUtil;
import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.utils.EnumUtils;
import com.gk.infra.enums.DomainEnum;
import com.gk.common.enums.MenuTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.config.service.SysParamsService;
import com.gk.meta.dto.SysMenuDTO;
import com.gk.meta.entity.SysMenuEntity;
import com.gk.infra.enums.ScopeEnum;
import com.gk.meta.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 * 
 * @author Lowen
 */
@RestController
@RequestMapping("/sys/menu")
@Tag(name = "菜单管理")
@AllArgsConstructor
public class SysMenuController {
    private final SysMenuService sysMenuService;
    private final SysParamsService sysParamsService;

	@GetMapping("nav")
	@Operation(summary = "导航")
	public R<?> nav(){
		List<SysMenuDTO> list = sysMenuService.getUserMenuList(MenuTypeEnum.enums(), Constant.MIN_SYS_ID);
        if(!ReqContextHolder.isSAdmin()){
            list.addFirst(sysMenuService.defaultNav());
        }
		return R.ok(list);
	}

	@GetMapping("list")
	@Operation(summary = "列表")
	@Parameter(name = "type", description = "菜单类型 0：菜单 1：按钮  null：全部", in = ParameterIn.QUERY)
	public R<?> list(@RequestParam(required = false) List<Integer> typeList){
		List<SysMenuDTO> list = sysMenuService.getUserMenuList(typeList, Constant.MIN_SYS_ID);
		return R.ok(list);
	}

	@GetMapping("{id}")
	@Operation(summary = "信息")
	@PreAuthorize("hasAuthority('sys:menu:info')")
	public R<?> get(@PathVariable("id") Long id){
		SysMenuDTO data = sysMenuService.get(id);
		return R.ok(data);
	}

    @PutMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('sys:menu:info')")
    public R<?> updateById(@PathVariable("id") Long id, @RequestBody SysMenuDTO dto){
        dto.setId(id);
        // 不是菜单组件为空
        if (ObjUtil.notEqual(MenuTypeEnum.MENU.code(), dto.getType())) {
            dto.setComponent("");
        }
        sysMenuService.update(dto);
        return R.ok();
    }

    @GetMapping("name-exists")
    @Operation(summary = "信息")
    public R<?> nameExists(@RequestMap DynMap dto){
        boolean rresult = sysMenuService.isExistsName(dto.getLong("id", 0L), dto.getStr("name"));
        return R.ok(rresult);
    }

    /**
     * 系统语言参数
     */
    @GetMapping("dict/scope")
    public R<?> dictScope(){
        return R.ok(EnumUtils.toDictList(ScopeEnum.class));
    }

    /**
     * 系统语言参数
     */
    @GetMapping("dict/domain")
    public R<?> dictDomain(){
        return R.ok(EnumUtils.toDictList(DomainEnum.class));
    }

	@PostMapping
	@Operation(summary = "保存")
	@PreAuthorize("hasAuthority('sys:menu:save')")
	public R<?> save(@RequestBody SysMenuDTO dto){
        SysMenuEntity entity = ConvertUtils.sourceToTarget(dto, SysMenuEntity.class);
        // 不是菜单组件为空
        if (ObjUtil.notEqual(MenuTypeEnum.MENU.code(), entity.getType())) {
            entity.setComponent("");
        }
		//效验数据
		sysMenuService.addMenu(entity);
		return R.ok();
	}

	@PutMapping
	@Operation(summary = "修改")
	@PreAuthorize("hasAuthority('sys:menu:update')")
	public R<?> update(@RequestBody SysMenuDTO dto){
        // 不是菜单组件为空
        if (ObjUtil.notEqual(MenuTypeEnum.MENU.code(), dto.getType())) {
            dto.setComponent("");
        }
		sysMenuService.update(dto);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Operation(summary = "删除")
	@PreAuthorize("hasAuthority('sys:menu:delete')")
	public R<?> delete(@PathVariable("id") Long id){
		//效验数据
		AssertUtils.isNull(id, "id");
		//判断是否有子菜单或按钮
		List<SysMenuDTO> list = sysMenuService.getListPid(id);
		if(list.size() > 0){
			return R.error(ErrorCode.SUB_MENU_EXIST);
		}
		sysMenuService.delete(id);
		return R.ok();
	}

	@GetMapping("select")
	@Operation(summary = "角色菜单权限")
	@PreAuthorize("hasAuthority('sys:menu:select')")
	public R<?> select(){
		List<SysMenuDTO> list = sysMenuService.getUserMenuList(null, Constant.MIN_SYS_ID);
		return R.ok(list);
	}
}