package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspRouteRuleDTO;
import com.gk.psp.service.PspRouteRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/psp/route-rule")
@RequiredArgsConstructor
public class PspRouteRuleController {
    private final PspRouteRuleService pspRouteRuleService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:route-rule:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspRouteRuleDTO> page = pspRouteRuleService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:route-rule:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspRouteRuleService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:route-rule:save')")
    public R<?> save(@RequestBody PspRouteRuleDTO dto) {
        pspRouteRuleService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('psp:route-rule:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspRouteRuleDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspRouteRuleService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:route-rule:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspRouteRuleService.delete(ids);
        return R.ok();
    }
}
