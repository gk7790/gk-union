package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.service.PspBankMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PSP银行映射")
@RestController
@RequestMapping("/psp/bank-mapping")
@RequiredArgsConstructor
public class PspBankMappingController {

    private final PspBankMappingService pspBankMappingService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "pspId", description = "PSP供应商ID", in = ParameterIn.QUERY),
            @Parameter(name = "countryCode", description = "国家代码", in = ParameterIn.QUERY),
            @Parameter(name = "currency", description = "币种", in = ParameterIn.QUERY),
            @Parameter(name = "bankCode", description = "平台标准银行编码", in = ParameterIn.QUERY),
            @Parameter(name = "pspBankCode", description = "PSP侧银行编码", in = ParameterIn.QUERY),
            @Parameter(name = "status", description = "状态：1启用 2暂停 3禁用", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('psp:bank-mapping:page')")
    public R<PageData<PspBankMappingDTO>> page(@RequestMap DynMap params) {
        return R.ok(pspBankMappingService.page(params));
    }

    @GetMapping("matrix")
    @Operation(summary = "对照列表", description = "按 PSP + 国家 + 币种返回平台银行与 PSP 映射对照列表，用于页面回显")
    @Parameters({
            @Parameter(name = "pspId", description = "PSP供应商ID", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "countryCode", description = "国家代码", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "currency", description = "币种", in = ParameterIn.QUERY, required = true)
    })
    @PreAuthorize("hasAuthority('psp:bank-mapping:page')")
    public R<List<PspBankMappingDTO>> matrix(@RequestParam Long pspId,
                                             @RequestParam String countryCode,
                                             @RequestParam String currency) {
        return R.ok(pspBankMappingService.getMatrix(pspId, countryCode, currency));
    }

    @PutMapping("matrix")
    @Operation(summary = "保存对照列表", description = "按行 diff 同步映射：有 PSP 编码 upsert，空 PSP 编码 delete")
    @PreAuthorize("hasAuthority('psp:bank-mapping:update')")
    public R<Void> saveMatrix(@RequestParam Long pspId,
                              @RequestParam String countryCode,
                              @RequestParam String currency,
                              @RequestBody List<PspBankMappingDTO> items) {
        pspBankMappingService.saveMatrix(pspId, countryCode, currency, items);
        return R.ok();
    }

    @GetMapping("{id}")
    @Operation(summary = "详情")
    @PreAuthorize("hasAuthority('psp:bank-mapping:info')")
    public R<PspBankMappingDTO> get(@PathVariable("id") Long id) {
        return R.ok(pspBankMappingService.get(id));
    }

    @PostMapping
    @Operation(summary = "新增")
    @PreAuthorize("hasAuthority('psp:bank-mapping:save')")
    public R<Void> save(@RequestBody PspBankMappingDTO dto) {
        pspBankMappingService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('psp:bank-mapping:update')")
    public R<Void> update(@PathVariable("id") Long id, @RequestBody PspBankMappingDTO dto) {
        AssertUtils.isReserved(id);
        dto.setMappingId(id);
        pspBankMappingService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('psp:bank-mapping:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspBankMappingService.delete(ids);
        return R.ok();
    }
}
