package com.gk.devtools.controller;

import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.devtools.entity.BaseClassEntity;
import com.gk.devtools.service.BaseClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 基类管理
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools/baseclass")
@Tag(name = "开发工具-基础类")
@RequiredArgsConstructor
public class BaseClassController {
    private final BaseClassService baseClassService;

    @GetMapping("page")
    public R<?> page(@RequestParam Map<String, Object> params){
        PageData<BaseClassEntity> page = baseClassService.page(params);
        return R.ok(page);
    }

    @GetMapping("list")
    public R<?> list(){
        List<BaseClassEntity> list = baseClassService.list();
        return R.ok(list);
    }

    @GetMapping("{id}")
    public R<?> get(@PathVariable("id") Long id){
        BaseClassEntity data = baseClassService.selectById(id);
        return R.ok(data);
    }

    @PostMapping
    public R<?> save(@RequestBody BaseClassEntity entity){
        baseClassService.insert(entity);
        return R.ok();
    }

    @PutMapping
    public R<?> update(@RequestBody BaseClassEntity entity){
        baseClassService.updateById(entity);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    public R<?> delete(@RequestParam Long id){
        //效验数据
        AssertUtils.isNull(id, "id");
        baseClassService.deleteById(id);
        return R.ok();
    }

}