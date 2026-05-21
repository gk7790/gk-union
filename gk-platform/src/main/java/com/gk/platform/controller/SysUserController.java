package com.gk.platform.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.page.PageData;
import com.gk.common.password.PasswordUtils;
import com.gk.common.tools.DataMap;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.dto.PasswordDTO;
import com.gk.platform.dto.SysUserDTO;
import com.gk.platform.entity.SysUserEntity;
import com.gk.platform.service.SysUserPostService;
import com.gk.platform.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 用户管理
 * 
 * @author Lowen
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {
    private final CurrentUser currentUser;
	private final SysUserService sysUserService;
	private final SysUserPostService sysUserPostService;
//	private final SysRoleUserService sysRoleUserService;

    /**
     * 分页
     */
	@GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true) ,
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true) ,
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY) ,
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY) ,
            @Parameter(name = "username", description = "用户名", in = ParameterIn.QUERY),
            @Parameter(name = "gender", description = "性别", in = ParameterIn.QUERY),
            @Parameter(name = "deptId", description = "部门ID", in = ParameterIn.QUERY)
    })
	@RequiresPermission("sys:user:page")
	public R<?> page(@RequestMap DataMap params){
		PageData<SysUserDTO> page = sysUserService.page(params);
		return R.ok(page);
	}

	@GetMapping("{id}")
	@Operation(summary = "信息")
	@RequiresPermission("sys:user:info")
	public R<?> get(@PathVariable("id") Long id){
		SysUserDTO data = sysUserService.getById(id);

		//用户角色列表
//		List<Long> roleIdList = sysRoleUserService.getRoleIdList(id);
//		data.setRoleIdList(roleIdList);

		//用户岗位列表
		List<Long> postIdList = sysUserPostService.getPostIdList(id);
		data.setPostIdList(postIdList);

		return R.ok(data);
	}

	@GetMapping("info")
	@Operation(summary = "登录用户信息")
	public R<?> info(){
        Long userId = currentUser.getUserId();

        SysUserDTO data = sysUserService.getById(userId);
		return R.ok(data);
	}

    @PutMapping("reset-password/{id}")
    @Operation(summary = "修改密码")
    public R<?> resetPassword(@PathVariable("id") Long id, @RequestBody SysUserDTO dto){
        //效验数据
        AssertUtils.isNull(id, "id");
        AssertUtils.isBlank(dto.getPassword(), "password");
        sysUserService.updatePassword(id, dto.getPassword());

        return R.ok();
    }

    @PutMapping("password")
	@Operation(summary = "修改密码")
	public R<?> password(@RequestBody PasswordDTO dto){
        SysUserEntity user = sysUserService.selectById(currentUser.getUserId());
		//原密码不正确
		if(!PasswordUtils.matches(dto.getPassword(), user.getPassword())){
			return R.error(ErrorCode.PASSWORD_ERROR);
		}

		sysUserService.updatePassword(user.getId(), dto.getNewPassword());

		return R.ok();
	}

	@PostMapping
	@Operation(summary = "保存")
	@RequiresPermission("sys:user:save")
	public R<?> save(@RequestBody SysUserDTO dto){
		sysUserService.save(dto);
		return R.ok();
	}

	@PutMapping
	@Operation(summary = "修改")
	@RequiresPermission("sys:user:update")
	public R<?> update(@RequestBody SysUserDTO dto){
		sysUserService.update(dto);
		return R.ok();
	}

	@PutMapping("app")
	public R<?> updateUserInfo(@RequestBody SysUserDTO dto){
		sysUserService.updateUserInfo(dto);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "删除")
	@RequiresPermission("sys:user:delete")
	public R<?> delete(@RequestParam Long[] ids){
		//效验数据
		AssertUtils.isArrayEmpty(ids, "id");

		List<Long> idList = Arrays.asList(ids);
		if(idList.contains(currentUser.getUserId())){
			throw new GkException(ErrorCode.DEL_MYSELF_ERROR);
		}
		sysUserService.deleteBatchIds(idList);
		return R.ok();
	}
}