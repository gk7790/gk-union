package com.gk.infra.dict.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.dict.dto.SysDictDataDTO;
import com.gk.infra.dict.service.SysDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 字典数据
 *
 * @author Lowen
 */
@RestController
@RequestMapping("sys/dict/data")
@Tag(name = "字典数据")
@AllArgsConstructor
public class SysDictDataController {
    private final SysDictDataService sysDictDataService;

    @GetMapping("page")
    @Operation(summary = "字典数据")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "dictLabel", description = "字典标签", in = ParameterIn.QUERY),
            @Parameter(name = "dictValue", description = "字典值", in = ParameterIn.QUERY)
    })
    @RequiresPermission("sys:dict:page")
    public R<?> page(@RequestMap DataMap params){
        //字典类型
        PageData<SysDictDataDTO> page = sysDictDataService.getPage(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("sys:dict:info")
    public R<?> get(@PathVariable("id") Long id){
        SysDictDataDTO data = sysDictDataService.get(id);
        return R.ok(data);
    }
    @PostMapping
    @Operation(summary = "保存")
    @RequiresPermission("sys:dict:save")
    public R<?> save(@RequestBody SysDictDataDTO dto){
        //效验数据
        sysDictDataService.save(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    @RequiresPermission("sys:dict:update")
    public R<?> update(@RequestBody SysDictDataDTO dto){
        //效验数据
        sysDictDataService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @RequiresPermission("sys:dict:delete")
    public R<?> delete(@RequestParam Long[] ids){
        //效验数据
        AssertUtils.isArrayEmpty(ids, "id");
        sysDictDataService.delete(ids);
        return R.ok();
    }

}