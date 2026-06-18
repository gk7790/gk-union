package com.gk.meta.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.meta.dto.SysCurrencyDTO;
import com.gk.meta.service.SysCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统-币种字典")
@RestController
@RequestMapping("/sys/currency")
@RequiredArgsConstructor
public class SysCurrencyController {

    private final SysCurrencyService sysCurrencyService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:currency:page')")
    public R<PageData<SysCurrencyDTO>> page(@RequestMap DynMap params) {
        return R.ok(sysCurrencyService.page(params));
    }

    @GetMapping("dict")
    @Operation(summary = "平台币种字典", description = "查询平台启用的币种列表，供下拉选择使用")
    public R<List<SysCurrencyDTO>> dict(@RequestMap DynMap params) {
        return R.ok(sysCurrencyService.getDict(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "详情")
    @PreAuthorize("hasAuthority('sys:currency:info')")
    public R<SysCurrencyDTO> get(@PathVariable("id") Long id) {
        return R.ok(sysCurrencyService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增")
    @PreAuthorize("hasAuthority('sys:currency:save')")
    public R<Void> save(@RequestBody SysCurrencyDTO dto) {
        sysCurrencyService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:currency:update')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody SysCurrencyDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        sysCurrencyService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:currency:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        sysCurrencyService.delete(ids);
        return R.ok();
    }
}
