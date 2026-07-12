package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.common.dto.LabelDTO;
import com.gk.psp.dto.PspMethodDictDTO;
import com.gk.psp.dto.PspMethodDTO;
import com.gk.psp.service.PspMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/psp/method")
@RequiredArgsConstructor
public class PspMethodController {
    private final PspMethodService pspMethodService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:method:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspMethodDTO> page = pspMethodService.page(params);
        return R.ok(page);
    }

    @GetMapping("dict-psp")
    public R<?> pspDict(@RequestMap DynMap params) {
        List<PspMethodDTO> list = pspMethodService.getPspMethodCodeDict(params);
        return R.ok(list);
    }

    @GetMapping("dict")
    public R<?> dict(@RequestMap DynMap params) {
        List<LabelDTO> list = pspMethodService.getMethodCodeDict(params);
        return R.ok(list);
    }

    @GetMapping("fee-rule-dict")
    public R<?> feeRuleDict(@RequestMap DynMap params) {
        List<PspMethodDictDTO> list = pspMethodService.getFeeRuleMethodDict(params);
        return R.ok(list);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:method:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspMethodService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('psp:method:save')")
    public R<?> save(@RequestBody PspMethodDTO dto) {
        pspMethodService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('psp:method:update')")
    public R<?> update(@PathVariable("id") Long id, @RequestBody PspMethodDTO dto) {
        AssertUtils.isReserved(id);
        dto.setId(id);
        pspMethodService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('psp:method:delete')")
    public R<?> delete(@RequestParam Long[] ids) {
        AssertUtils.isArrayEmpty(ids, "id");
        pspMethodService.delete(ids);
        return R.ok();
    }
}
