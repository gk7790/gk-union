package com.gk.meta.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.meta.dto.SysBankDTO;
import com.gk.meta.service.SysBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统-标准银行")
@RestController
@RequestMapping("/sys/bank")
@RequiredArgsConstructor
public class SysBankController {

    private final SysBankService sysBankService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:bank:page')")
    public R<PageData<SysBankDTO>> page(@RequestMap DynMap params) {
        return R.ok(sysBankService.page(params));
    }

    @GetMapping("list")
    @Operation(summary = "集合")
    @PreAuthorize("hasAuthority('sys:bank:list')")
    public R<List<SysBankDTO>> list(@RequestMap DynMap params) {
        return R.ok(sysBankService.list(params));
    }


    @GetMapping("dict")
    @Operation(summary = "银行字典", description = "按国家/币种查询可用银行列表")
    @Parameters({
            @Parameter(name = "countryCode", description = "国家代码，如 PH", in = ParameterIn.QUERY),
            @Parameter(name = "currency", description = "币种，如 PHP", in = ParameterIn.QUERY)
    })
    public R<List<SysBankDTO>> dict(@RequestMap DynMap params) {
        return R.ok(sysBankService.getDict(params));
    }

    @GetMapping("{id}")
    @Operation(summary = "详情")
    @PreAuthorize("hasAuthority('sys:bank:info')")
    public R<SysBankDTO> get(@PathVariable("id") Long id) {
        return R.ok(sysBankService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增")
    @PreAuthorize("hasAuthority('sys:bank:save')")
    public R<Void> save(@RequestBody SysBankDTO dto) {
        sysBankService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:bank:update')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody SysBankDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        sysBankService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:bank:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        sysBankService.delete(ids);
        return R.ok();
    }
}
