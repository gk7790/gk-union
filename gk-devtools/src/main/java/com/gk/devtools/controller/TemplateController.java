package com.gk.devtools.controller;

import cn.hutool.core.map.MapUtil;
import com.gk.common.exception.GkException;
import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.devtools.entity.TemplateEntity;
import com.gk.devtools.service.TemplateService;
import com.gk.devtools.utils.GenUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * 模板管理
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools/template")
@Tag(name = "开发工具-模版")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;

    @GetMapping("page")
    public R<?> page(@RequestParam Map<String, Object> params){
        PageData<TemplateEntity> page = templateService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    public R<?> get(@PathVariable("id") Long id){
        TemplateEntity data = templateService.selectById(id);

        return R.ok(data);
    }

    @PostMapping
    public R<?> save(@RequestBody TemplateEntity entity){
        try {
            entity.setContent(HtmlUtils.htmlUnescape(entity.getContent()));

            //检查模板语法是否正确
            GenUtils.getTemplateContent(entity.getContent(), MapUtil.newHashMap());
        } catch (Exception e) {
            throw new GkException("模板语法错误，请查看控制台报错信息！", e);
        }

        templateService.insert(entity);

        return R.ok();
    }

    @PutMapping
    public R<?> update(@RequestBody TemplateEntity entity){
        try {
            entity.setContent(HtmlUtils.htmlUnescape(entity.getContent()));
            //检查模板语法是否正确
            GenUtils.getTemplateContent(entity.getContent(),  MapUtil.newHashMap());
        } catch (Exception e) {
            throw new GkException("模板语法错误，请查看控制台报错信息！", e);
        }
        templateService.updateById(entity);

        return R.ok();
    }

    @DeleteMapping
    public R<?> delete(@RequestBody Long[] ids){
        templateService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 启用
     */
    @PutMapping("enabled")
    public R<?> enabled(@RequestBody Long[] ids){
        templateService.updateStatusBatch(ids, 0);

        return R.ok();
    }

    /**
     * 禁用
     */
    @PutMapping("disabled")
    public R<?> disabled(@RequestBody Long[] ids){
        templateService.updateStatusBatch(ids, 1);

        return R.ok();
    }
}