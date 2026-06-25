package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.psp.dto.PspCallbackLogDTO;
import com.gk.psp.service.PspCallbackLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/psp/callback-log")
@RequiredArgsConstructor
public class PspCallbackLogController {
    private final PspCallbackLogService pspCallbackLogService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:callback-log:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspCallbackLogDTO> page = pspCallbackLogService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:callback-log:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspCallbackLogService.get(id));
    }
}
