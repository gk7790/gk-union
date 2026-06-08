package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspProviderDTO;
import com.gk.psp.service.PspProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PSP管理")
@RestController
@RequestMapping("/psp/provider")
@RequiredArgsConstructor
public class PspProviderController {
    private final PspProviderService pspProviderService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @RequiresPermission("psp:provider:page")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspProviderDTO> page = pspProviderService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("psp:provider:info")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspProviderService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @RequiresPermission("psp:provider:save")
    public R<?> save(@RequestBody PspProviderDTO dto) {
        pspProviderService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @RequiresPermission("psp:provider:update")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspProviderDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspProviderService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @RequiresPermission("psp:provider:delete")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspProviderService.delete(ids);
        return R.ok();
    }
}
