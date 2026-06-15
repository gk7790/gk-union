package com.gk.merchant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.merchant.dto.MerchantDTO;
import com.gk.merchant.service.MerchantService;
import com.gk.merchant.support.MerchantTgBindCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商户管理")
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;
    private final MerchantTgBindCodeService merchantTgBindCodeService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('merchant:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<MerchantDTO> page = merchantService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('merchant:info')")
    public R<?> get(@PathVariable("id") Long id) {
        MerchantDTO data = merchantService.get(id);
        Long subjectId = ReqContextHolder.getSubjectId();
        Long merchantId = ReqContextHolder.getMerchantId();
        if (data != null && data.getId() != null && subjectId != null && data.getId().equals(merchantId)) {
            data.setTgBindCode(merchantTgBindCodeService.generate(subjectId));
        }
        return R.ok(data);
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('merchant:save')")
    public R<?> save(@RequestBody MerchantDTO dto) {
        merchantService.save(dto);
        return R.ok(dto);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('merchant:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody MerchantDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        merchantService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('merchant:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        merchantService.delete(ids);
        return R.ok();
    }
}
