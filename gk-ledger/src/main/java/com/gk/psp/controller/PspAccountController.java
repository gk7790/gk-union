package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.service.PspAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PSP账户")
@RestController
@RequestMapping("/psp/account")
@RequiredArgsConstructor
public class PspAccountController {
    private final PspAccountService pspAccountService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @RequiresPermission("psp:account:page")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspAccountDTO> page = pspAccountService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("psp:account:info")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspAccountService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @RequiresPermission("psp:account:save")
    public R<?> save(@RequestBody PspAccountDTO dto) {
        pspAccountService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @RequiresPermission("psp:account:update")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspAccountDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspAccountService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @RequiresPermission("psp:account:delete")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspAccountService.delete(ids);
        return R.ok();
    }
}
