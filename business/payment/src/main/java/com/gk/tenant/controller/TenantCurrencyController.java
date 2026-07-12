package com.gk.tenant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.reference.dto.SysCurrencyDTO;
import com.gk.tenant.dto.TenantCurrencyDTO;
import com.gk.tenant.service.TenantCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统-租户币种")
@RestController
@RequestMapping("/sys/tenant-currency")
@RequiredArgsConstructor
public class TenantCurrencyController {

    private final TenantCurrencyService tenantCurrencyService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "tenantId", description = "租户ID，平台登录时可传", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:tenant-currency:page')")
    public R<PageData<TenantCurrencyDTO>> page(@RequestMap DynMap params) {
        return R.ok(tenantCurrencyService.page(params));
    }

    @GetMapping("dict")
    @Operation(summary = "租户可用币种字典", description = "返回当前租户已启用的币种及币种详情。租户登录自动取当前租户；平台登录需tenantId")
    @Parameters({
            @Parameter(name = "tenantId", description = "租户ID，平台登录时必填", in = ParameterIn.QUERY)
    })
    public R<List<SysCurrencyDTO>> dict(@RequestMap DynMap params) {
        return R.ok(tenantCurrencyService.getDict(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "详情")
    @PreAuthorize("hasAuthority('sys:tenant-currency:info')")
    public R<TenantCurrencyDTO> get(@PathVariable("id") Long id) {
        return R.ok(tenantCurrencyService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增")
    @PreAuthorize("hasAuthority('sys:tenant-currency:save')")
    public R<Void> save(@RequestBody TenantCurrencyDTO dto) {
        tenantCurrencyService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:tenant-currency:update')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody TenantCurrencyDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        tenantCurrencyService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:tenant-currency:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        tenantCurrencyService.delete(ids);
        return R.ok();
    }
}
