package com.gk.iam.controller;


import cn.hutool.core.util.ObjUtil;
import com.gk.common.annotation.RequestMap;
import com.gk.common.beans.CurrentUser;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.common.model.PageData;
import com.gk.common.password.PasswordUtils;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.dto.PasswordDTO;
import com.gk.iam.dto.SysUserDTO;
import com.gk.iam.entity.SysUserEntity;
import com.gk.iam.entity.SysUserSubjectEntity;
import com.gk.iam.service.SysRoleUserService;
import com.gk.iam.service.SysUserService;
import com.gk.iam.service.SysUserSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 用户管理
 *
 * @author Lowen
 */
@Tag(name = "系统-用户管理", description = "维护后台登录账号，并通过 sys_user_subject 绑定平台、租户或商户主体")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {
    private final CurrentUser currentUser;
    private final SysUserService sysUserService;
    private final SysUserSubjectService sysUserSubjectService;
    private final SysRoleUserService sysRoleUserService;

    /**
     * 分页
     */
    @GetMapping("page")
    @Operation(summary = "用户分页", description = "分页查询后台用户。非平台主体只能看到自身数据范围内的用户。权限码：sys:user:page。")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
        @Parameter(name = "username", description = "用户名", in = ParameterIn.QUERY),
        @Parameter(name = "gender", description = "性别", in = ParameterIn.QUERY),
        @Parameter(name = "deptId", description = "部门ID", in = ParameterIn.QUERY),
        @Parameter(name = "tenantId", description = "租户ID，仅平台主体可传", in = ParameterIn.QUERY),
        @Parameter(name = "merchantId", description = "商户ID，仅平台主体可传", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:user:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<SysUserDTO> page = sysUserService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "用户详情", description = "查询用户基础信息及主体绑定角色。权限码：sys:user:info。")
    @PreAuthorize("hasAuthority('sys:user:info')")
    public R<?> get(@PathVariable("id") Long id) {
        SysUserDTO data = sysUserService.getById(id);
        fillUserContext(data);

        return R.ok(data);
    }

    @GetMapping("info")
    @Operation(summary = "当前登录用户信息", description = "查询当前登录用户的基础资料和主体上下文。需要登录。")
    public R<?> info() {
        Long userId = currentUser.getUserId();
        SysUserDTO data = sysUserService.getById(userId);
        fillCurrentUserContext(data);
        return R.ok(data);
    }

    @PutMapping("reset-password/{id}")
    @Operation(summary = "重置用户密码", description = "管理员为指定用户重置密码。权限码：sys:user:update。")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public R<?> resetPassword(@PathVariable("id") Long id, @RequestBody SysUserDTO dto) {
        //效验数据
        AssertUtils.isNull(id, "id");
        AssertUtils.isBlank(dto.getPassword(), "password");
        sysUserService.updatePassword(id, dto.getPassword());

        return R.ok();
    }

    @PutMapping("password")
    @Operation(summary = "修改当前用户密码", description = "当前登录用户修改自己的密码，需要提供原密码和新密码。")
    public R<?> password(@Valid @RequestBody PasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return R.error(ErrorCode.PASSWORD_INCONSISTENCY);
        }
        if (dto.getPassword().equals(dto.getNewPassword())) {
            return R.errorMsg(ErrorCode.FAILURE, "新密码不能与原密码相同");
        }

        SysUserEntity user = sysUserService.selectById(currentUser.getUserId());
        //原密码不正确
        if (!PasswordUtils.matches(dto.getPassword(), user.getPassword())) {
            return R.error(ErrorCode.PASSWORD_ERROR);
        }

        sysUserService.updatePassword(user.getId(), dto.getNewPassword());

        return R.ok();
    }

    @PostMapping
    @Operation(summary = "新增用户", description = "创建登录账号并绑定主体、部门和角色。平台可指定租户/商户；租户使用当前租户；商户使用当前租户和商户。权限码：sys:user:add。")
    @PreAuthorize("hasAuthority('sys:user:add')")
    public R<?> save(@RequestBody SysUserDTO dto) {
        applySubjectContext(dto);
        if (ObjUtil.isEmpty(dto.getDeptId())) {
            dto.setDeptId(ReqContextHolder.getDeptId());
        }
        sysUserService.save(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改用户", description = "修改用户资料和主体绑定信息。租户/商户主体不可变更租户与商户归属范围。权限码：sys:user:update。")
    @PreAuthorize("hasAuthority('sys:user:update')")
    public R<?> update(@RequestBody SysUserDTO dto) {
        applySubjectContext(dto);
        boolean allowed = ReqContextHolder.isSuperAdmin() || currentUser.hasAllRole("admin");
        if (!allowed) {
            dto.setDeptId(null);
        }
        sysUserService.update(dto);
        return R.ok();
    }

    @PutMapping("app")
    @Operation(summary = "修改当前用户资料", description = "当前登录用户修改头像、昵称、姓名、性别、手机号、邮箱等个人资料；用户名不支持自助修改。")
    public R<?> updateUserInfo(@RequestBody SysUserDTO dto) {
        if (dto == null) {
            dto = new SysUserDTO();
        }
        Long userId = currentUser.getUserId();
        if (userId == null) {
            userId = ReqContextHolder.getUserId();
        }
        AssertUtils.isNull(userId, "userId");
        dto.setId(userId);
        sysUserService.updateUserInfo(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除用户", description = "删除用户账号。禁止删除当前登录用户自身。权限码：sys:user:delete。")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        //效验数据
        AssertUtils.isArrayEmpty(ids, "id");

        List<Long> idList = Arrays.asList(ids);
        if (idList.contains(currentUser.getUserId())) {
            throw new GkException(ErrorCode.DEL_MYSELF_ERROR);
        }
        sysUserService.deleteBatchIds(idList);
        return R.ok();
    }

    /**
     * 按当前登录主体范围约束 tenantId / merchantId。
     * <ul>
     *     <li>平台：以前端传入为准</li>
     *     <li>租户：tenantId 固定为当前租户，merchantId 以前端传入为准</li>
     *     <li>商户：tenantId、merchantId 均固定为当前登录主体</li>
     * </ul>
     */
    private void applySubjectContext(SysUserDTO dto) {
        if (ReqContextHolder.isPlatform()) {
            return;
        }
        if (SubjectTypeEnum.MERCHANT.matches(ReqContextHolder.getSubjectType())) {
            dto.setTenantId(ReqContextHolder.getTenantId());
            dto.setMerchantId(ReqContextHolder.getMerchantId());
            return;
        }
        dto.setTenantId(ReqContextHolder.getTenantId());
    }

    private void fillUserContext(SysUserDTO data) {
        if (data == null || data.getId() == null) {
            return;
        }
        if (data.getRoleId() != null) {
            data.setRoleIdList(List.of(data.getRoleId()));
        }

        SysUserSubjectEntity userSubject = sysUserSubjectService.getByUserId(data.getId());
        if (ObjUtil.isEmpty(userSubject)) {
            return;
        }

        data.setSubjectId(userSubject.getId());
        data.setSubjectType(userSubject.getSubjectType());
        data.setDeptId(userSubject.getDeptId());
        data.setTenantId(userSubject.getTenantId());
        data.setMerchantId(userSubject.getMerchantId());

        List<Long> roleIdList = sysRoleUserService.getRoleIdListBySubjectId(userSubject.getId());
        data.setRoleIdList(roleIdList);
        if (roleIdList != null && !roleIdList.isEmpty()) {
            data.setRoleId(roleIdList.get(0));
        }
    }

    private void fillCurrentUserContext(SysUserDTO data) {
        if (data == null || ReqContextHolder.getSubjectId() == null) {
            return;
        }
        data.setSubjectId(ReqContextHolder.getSubjectId());
        data.setSubjectType(ReqContextHolder.getSubjectType());
    }
}
