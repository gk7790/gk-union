package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.psp.dto.PspAccountDTO;
import com.gk.psp.service.PspAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/psp/account")
@RequiredArgsConstructor
public class PspAccountController {
    private final PspAccountService pspAccountService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:account:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspAccountDTO> page = pspAccountService.page(params);
        return R.ok(page);
    }

    @GetMapping("dict")
    public R<List<LabelDTO>> dict(@RequestMap DynMap params) {
        return R.ok(pspAccountService.getDict(params));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:account:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspAccountService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:account:save')")
    public R<?> save(@RequestBody PspAccountDTO dto) {
        pspAccountService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('psp:account:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspAccountDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspAccountService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:account:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspAccountService.delete(ids);
        return R.ok();
    }
}
