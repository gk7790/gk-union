package com.gk.platform.controller;

import cn.hutool.core.util.ObjUtil;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.platform.dto.SysDeptDTO;
import com.gk.platform.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "部门管理")
@RequiredArgsConstructor
public class SysDeptController {
    private final SysDeptService sysDeptService;

    @GetMapping("list")
    @Operation(summary = "列表")
    @PreAuthorize("hasAuthority('sys:dept:view')")
    public R<?> list() {
        List<SysDeptDTO> list = sysDeptService.list(new HashMap<>());
        return R.ok(list);
    }

    @PostMapping
    @Operation(summary = "保存")
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
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:dept:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody SysDeptDTO dto) {
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
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:dept:delete')")
    public R<?> delete(@PathVariable("id") Long id) {
        //效验数据
        AssertUtils.isNull(id, "id");
        if(id <= 100){
            return R.error(ErrorCode.FORBIDDEN);
        }
        sysDeptService.delete(id);
        return R.ok();
    }

}
