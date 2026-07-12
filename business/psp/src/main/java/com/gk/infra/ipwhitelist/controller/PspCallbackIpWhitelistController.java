package com.gk.infra.ipwhitelist.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.ipwhitelist.dto.PspCallbackIpWhitelistDTO;
import com.gk.infra.ipwhitelist.service.PspCallbackIpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PSP回调IP白名单")
@RestController
@RequestMapping("psp/callback-ip-whitelist")
@RequiredArgsConstructor
public class PspCallbackIpWhitelistController {
    private final PspCallbackIpWhitelistService pspCallbackIpWhitelistService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('psp:callback-ip-whitelist:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspCallbackIpWhitelistDTO> page = pspCallbackIpWhitelistService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @PreAuthorize("hasAuthority('psp:callback-ip-whitelist:info')")
    public R<?> get(@PathVariable Long id) {
        return R.ok(pspCallbackIpWhitelistService.get(id));
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('psp:callback-ip-whitelist:save')")
    public R<?> save(@RequestBody PspCallbackIpWhitelistDTO dto) {
        pspCallbackIpWhitelistService.save(dto);
        return R.ok(dto);
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('psp:callback-ip-whitelist:update')")
    public R<?> update(@PathVariable Long id, @RequestBody PspCallbackIpWhitelistDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspCallbackIpWhitelistService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('psp:callback-ip-whitelist:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspCallbackIpWhitelistService.delete(ids);
        return R.ok();
    }
}
