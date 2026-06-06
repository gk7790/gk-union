package com.gk.platform.controller;

import com.gk.infra.config.service.SysParamsService;
import com.gk.platform.service.SysI18nService;
import com.gk.platform.service.SysUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("sys/timezone")
@Tag(name = "系统-国际化")
@RequiredArgsConstructor
public class SysTimezoneController {
    private final SysParamsService sysParamsService;
    private final SysUserService sysUserService;


}
