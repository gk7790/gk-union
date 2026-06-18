package com.gk.infra.controller;

import com.gk.common.context.ReqContextHolder;
import com.gk.common.dto.LabelDTO;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.model.R;
import com.gk.common.provider.EnumDictProvider;
import com.gk.infra.i18n.service.I18nService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
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
    private final EnumDictProvider enumDictProvider;

    @GetMapping("/i18n/{key}")
    public R<?> getI18nList(@PathVariable("key") String key, @RequestParam String lang) {
        Map<String, Object> messages = i18nService.getMessages(key, lang);
        return R.ok(messages);
    }


    @GetMapping("/enum/{key}")
    public R<?> getEnumDict(@PathVariable("key") String key) {
        if ("subjectType".equalsIgnoreCase(key)) {
            return R.ok(SubjectTypeEnum.visibleList(ReqContextHolder.getSubjectType()));
        }
        return R.ok(enumDictProvider.get(key));
    }

    @GetMapping("/enum/list")
    public R<?> getEnumDictList(@RequestParam(required = false) List<String> keys) {
        Map<String, List<LabelDTO>> result = new LinkedHashMap<>(enumDictProvider.list(keys));
        if (containsKey(keys, "subjectType")) {
            result.put("subjectType", SubjectTypeEnum.visibleList(ReqContextHolder.getSubjectType()));
        }
        return R.ok(result);
    }

    private boolean containsKey(List<String> keys, String key) {
        if (keys == null || keys.isEmpty()) {
            return true;
        }
        for (String item : keys) {
            if (key.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
}
