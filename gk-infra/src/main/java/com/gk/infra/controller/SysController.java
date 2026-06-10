package com.gk.infra.controller;

import com.gk.common.model.R;
import com.gk.common.utils.EnumUtils;
import com.gk.infra.enums.DomainEnum;
import com.gk.infra.enums.ScopeEnum;
import com.gk.infra.i18n.service.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.util.Map;


/**
* 系统-国际化
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-23
*/
@RestController
@RequestMapping()
@Tag(name = "系统-国际化")
@RequiredArgsConstructor
public class SysController {
    private final I18nService i18nService;

    @GetMapping("/i18n/{key}")
    public R<?> getI18nList(@PathVariable("key") String key, @RequestParam String lang) {
        Map<String, Object> messages = i18nService.getMessages(key, lang);
        return R.ok(messages);
    }


    @GetMapping("/enum/{key}")
    public R<?> getEnumDict(@PathVariable("key") String key) {
        if ("scope".equalsIgnoreCase(key)) {
            return R.ok(EnumUtils.toDictList(ScopeEnum.class));
        } else if ("domain".equalsIgnoreCase(key)) {
            return R.ok(EnumUtils.toDictList(DomainEnum.class));
        }
        return R.ok();
    }
}