package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspProviderDTO;
import com.gk.psp.service.PspProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/psp/provider")
@RequiredArgsConstructor
public class PspProviderController {
    private final PspProviderService pspProviderService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:provider:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspProviderDTO> page = pspProviderService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:provider:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspProviderService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:provider:save')")
    public R<?> save(@RequestBody PspProviderDTO dto) {
        pspProviderService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('psp:provider:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspProviderDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspProviderService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:provider:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspProviderService.delete(ids);
        return R.ok();
    }
}
