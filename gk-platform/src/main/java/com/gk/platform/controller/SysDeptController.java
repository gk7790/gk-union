package com.gk.platform.controller;

import cn.hutool.core.util.ObjUtil;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.platform.dto.SysDeptDTO;
import com.gk.platform.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * 部门管理
 *
 * @author Lowen
 */
@RestController
@RequestMapping("/sys/dept")
@Tag(name = "系统-部门管理", description = "维护平台或租户内部部门，用于用户归属和部门数据权限")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SysDeptController {
    private final SysDeptService sysDeptService;

    @GetMapping("list")
    @Operation(summary = "部门列表", description = "查询当前主体可见的部门树。租户主体只能看到本租户部门。权限码：sys:dept:view。")
    @PreAuthorize("hasAuthority('sys:dept:view')")
    public R<?> list() {
        List<SysDeptDTO> list = sysDeptService.list(new HashMap<>());
        return R.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增部门", description = "新增部门。租户主体新增时后端自动写入当前租户ID。权限码：sys:dept:add。")
    @PreAuthorize("hasAuthority('sys:dept:add')")
    public R<?> save(@RequestBody SysDeptDTO dto) {
        if (!ReqContextHolder.isSAdmin()) {
            dto.setTenantId(ReqContextHolder.getTenantId());
            if (ObjUtil.isEmpty(dto.getPid()) || dto.getPid() <= Constant.MAX_RESERVED_ID) {
                dto.setPid(ReqContextHolder.getDeptId());
            }
        }
        //效验数据
        sysDeptService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改部门", description = "修改部门资料或上级部门。非平台主体只能操作自身数据范围内的部门。权限码：sys:dept:update。")
    @PreAuthorize("hasAuthority('sys:dept:update')")
    public R<?> update(@Parameter(description = "部门ID", required = true) @PathVariable("id") Long id, @RequestBody SysDeptDTO dto) {
        dto.setId(id);
        if (!ReqContextHolder.isSAdmin()) {
            dto.setTenantId(null);
            if (ObjUtil.isEmpty(dto.getPid()) || dto.getPid() <= Constant.MAX_RESERVED_ID) {
                dto.setPid(ReqContextHolder.getDeptId());
            }
        }
        //效验数据
        sysDeptService.update(dto);
        return R.ok();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "删除部门", description = "删除部门前会校验是否存在子部门或部门用户。权限码：sys:dept:delete。")
    @PreAuthorize("hasAuthority('sys:dept:delete')")
    public R<?> delete(@Parameter(description = "部门ID", required = true) @PathVariable("id") Long id) {
        //效验数据
        AssertUtils.isNull(id, "id");
        if(id <= 100){
            return R.error(ErrorCode.FORBIDDEN);
        }
        sysDeptService.delete(id);
        return R.ok();
    }

}
