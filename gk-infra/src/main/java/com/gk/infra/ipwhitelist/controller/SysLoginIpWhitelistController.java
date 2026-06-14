package com.gk.infra.ipwhitelist.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.ipwhitelist.dto.SysLoginIpWhitelistDTO;
import com.gk.infra.ipwhitelist.service.SysLoginIpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录IP白名单")
@RestController
@RequestMapping("sys/login-ip-whitelist")
@RequiredArgsConstructor
public class SysLoginIpWhitelistController {
    private final SysLoginIpWhitelistService sysLoginIpWhitelistService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:login-ip-whitelist:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<SysLoginIpWhitelistDTO> page = sysLoginIpWhitelistService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('sys:login-ip-whitelist:info')")
    public R<?> get(@PathVariable Long id) {
        return R.ok(sysLoginIpWhitelistService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('sys:login-ip-whitelist:save')")
    public R<?> save(@RequestBody SysLoginIpWhitelistDTO dto) {
        sysLoginIpWhitelistService.save(dto);
        return R.ok(dto);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:login-ip-whitelist:update')")
    public R<?> update(@PathVariable Long id, @RequestBody SysLoginIpWhitelistDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        sysLoginIpWhitelistService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:login-ip-whitelist:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        sysLoginIpWhitelistService.delete(ids);
        return R.ok();
    }
}
