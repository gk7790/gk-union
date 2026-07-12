package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspFeeRuleDTO;
import com.gk.psp.service.PspFeeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/psp/fee-rule")
@RequiredArgsConstructor
public class PspFeeRuleController {
    private final PspFeeRuleService pspFeeRuleService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:fee-rule:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspFeeRuleDTO> page = pspFeeRuleService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:fee-rule:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspFeeRuleService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:fee-rule:save')")
    public R<?> save(@RequestBody PspFeeRuleDTO dto) {
        pspFeeRuleService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('psp:fee-rule:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspFeeRuleDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspFeeRuleService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:fee-rule:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspFeeRuleService.delete(ids);
        return R.ok();
    }
}
