package com.gk.infra.i18n.controller;

import com.gk.common.tools.R;
import com.gk.infra.i18n.service.I18nService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Map;


/**
* 系统-国际化
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-23
*/
@RestController
@RequestMapping("i18n")
@Tag(name = "系统-国际化")
@RequiredArgsConstructor
public class I18nController {
    private final I18nService i18nService;

    @GetMapping("menu")
    public R<?> getMenu(String lang) {
        Map<String, Object> messages = i18nService.getMessages("menu", lang);
        return R.ok(messages);
    }


    @GetMapping("dict")
    public R<?> getDict(String lang) {
        Map<String, Object> messages = i18nService.getMessages("dict", lang);
        return R.ok(messages);
    }

}