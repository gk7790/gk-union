package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspBankMappingDTO;
import com.gk.psp.service.PspBankMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/psp/bank-mapping")
@RequiredArgsConstructor
public class PspBankMappingController {

    private final PspBankMappingService pspBankMappingService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:bank-mapping:page')")
    public R<PageData<PspBankMappingDTO>> page(@RequestMap DynMap params) {
        return R.ok(pspBankMappingService.page(params));
    }

    @GetMapping("matrix")
    @PreAuthorize("hasAuthority('psp:bank-mapping:page')")
    public R<List<PspBankMappingDTO>> matrix(@RequestParam Long pspId,
                                             @RequestParam String countryCode,
                                             @RequestParam(required = false) String currency) {
        return R.ok(pspBankMappingService.getMatrix(pspId, countryCode, currency));
    }

    @PutMapping("matrix")
    @PreAuthorize("hasAuthority('psp:bank-mapping:update')")
    public R<Void> saveMatrix(@RequestParam Long pspId,
                              @RequestParam String countryCode,
                              @RequestParam(required = false) String currency,
                              @RequestBody List<PspBankMappingDTO> items) {
        pspBankMappingService.saveMatrix(pspId, countryCode, currency, items);
        return R.ok();
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:bank-mapping:info')")
    public R<PspBankMappingDTO> get(@PathVariable("id") Long id) {
        return R.ok(pspBankMappingService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:bank-mapping:save')")
    public R<Void> save(@RequestBody PspBankMappingDTO dto) {
        pspBankMappingService.save(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:bank-mapping:delete')")
    public R<Void> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspBankMappingService.delete(ids);
        return R.ok();
    }
}
