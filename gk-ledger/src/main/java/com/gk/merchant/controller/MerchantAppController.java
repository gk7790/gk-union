package com.gk.merchant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.merchant.dto.MerchantAppDTO;
import com.gk.merchant.service.MerchantAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商户应用管理")
@RestController
@RequestMapping("/merchant/app")
@RequiredArgsConstructor
public class MerchantAppController {
    private final MerchantAppService merchantAppService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('merchant:app:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantAppDTO> page = merchantAppService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('merchant:app:info')")
    public R<?> get(@PathVariable("id") Long id) {
        MerchantAppDTO data = merchantAppService.get(id);
        return R.ok(data);
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('merchant:app:save')")
    public R<?> save(@RequestBody MerchantAppDTO dto) {
        merchantAppService.save(dto);
        return R.ok(dto);
    }

    @GetMapping("dict")
    @Operation(summary = "保存")
    public R<?> dict(@RequestMap DynMap params) {
        List<MerchantAppDTO> list = merchantAppService.getDict(params);
        return R.ok(list);
    }

    @PostMapping("{id}/reset-secret")
    @Operation(summary = "重置API密钥")
    @PreAuthorize("hasAuthority('merchant:app:update')")
    public R<?> resetSecret(@PathVariable("id") Long id) {
        AssertUtils.isReserved(id);
        return R.ok(merchantAppService.resetApiSecret(id));
    }

    @PostMapping("{id}/production")
    @Operation(summary = "创建正式APP")
    @PreAuthorize("hasAuthority('merchant:app:update')")
    public R<?> createProductionApp(@PathVariable("id") Long id) {
        AssertUtils.isReserved(id);
        return R.ok(merchantAppService.createProductionApp(id));
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('merchant:app:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody MerchantAppDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        merchantAppService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('merchant:app:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        merchantAppService.delete(ids);
        return R.ok();
    }
}
