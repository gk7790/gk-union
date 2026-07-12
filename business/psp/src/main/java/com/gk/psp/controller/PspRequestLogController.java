package com.gk.psp.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.model.DynMap;
import com.gk.common.model.PageData;
import com.gk.common.model.R;
import com.gk.psp.dto.PspRequestLogDTO;
import com.gk.psp.service.PspRequestLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/psp/request-log")
@RequiredArgsConstructor
public class PspRequestLogController {
    private final PspRequestLogService pspRequestLogService;

    @GetMapping("page")
    @PreAuthorize("hasAuthority('psp:request-log:page')")
    public R<?> page(@RequestMap DynMap params) {
        PageData<PspRequestLogDTO> page = pspRequestLogService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('psp:request-log:info')")
    public R<?> get(@PathVariable("id") Long id) {
        return R.ok(pspRequestLogService.get(id));
    }
}
