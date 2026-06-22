package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.common.dto.LabelDTO;
import com.gk.psp.dto.PspMethodDictDTO;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.service.PspMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PSP支付方式")
@RestController
@RequestMapping("/psp/method")
@RequiredArgsConstructor
public class PspMethodController {
    private final PspMethodService pspMethodService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('psp:method:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspMethodDTO> page = pspMethodService.page(params);
        return R.ok(page);
    }

    @GetMapping("dict")
    @Operation(summary = "支付方式字典")
    public R<?> dict(@RequestMap DynMap params) {
        List<LabelDTO> list = pspMethodService.getMethodCodeDict(params);
        return R.ok(list);
    }

    @GetMapping("fee-rule-dict")
    @Operation(summary = "PSP成本规则支付方式字典")
    public R<?> feeRuleDict(@RequestMap DynMap params) {
        List<PspMethodDictDTO> list = pspMethodService.getFeeRuleMethodDict(params);
        return R.ok(list);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('psp:method:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspMethodService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('psp:method:save')")
    public R<?> save(@RequestBody PspMethodDTO dto) {
        pspMethodService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('psp:method:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspMethodDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspMethodService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('psp:method:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspMethodService.delete(ids);
        return R.ok();
    }
}
