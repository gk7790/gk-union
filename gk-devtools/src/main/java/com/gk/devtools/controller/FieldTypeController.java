package com.gk.devtools.controller;

import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.devtools.entity.FieldTypeEntity;
import com.gk.devtools.service.FieldTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 字段类型管理
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools/field-type")
@Tag(name = "开发工具-字段类型")
@RequiredArgsConstructor
public class FieldTypeController {
    private final FieldTypeService fieldTypeService;

    @GetMapping("page")
    public R<?> page(@RequestParam Map<String, Object> params){
        PageData<FieldTypeEntity> page = fieldTypeService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    public R<?> get(@PathVariable("id") Long id){
        FieldTypeEntity data = fieldTypeService.selectById(id);
        return R.ok(data);
    }

    @GetMapping("list")
    public R<?> list(){
        Set<String> set = fieldTypeService.list();
        return R.ok(set);
    }

    @PostMapping
    public R<?> save(@RequestBody FieldTypeEntity entity){
        fieldTypeService.insert(entity);
        return R.ok();
    }

    @PutMapping
    public R<?> update(@RequestBody FieldTypeEntity entity){
        fieldTypeService.updateById(entity);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    public R<?> delete(@RequestParam Long id){
        //效验数据
        AssertUtils.isNull(id, "id");
        fieldTypeService.deleteById(id);
        return R.ok();
    }
}