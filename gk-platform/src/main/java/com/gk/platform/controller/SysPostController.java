

package com.gk.platform.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.platform.dto.SysPostDTO;
import com.gk.platform.service.SysPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
* 岗位管理
*
* @author Mark sunlightcs@gmail.com
*/
@RestController
@RequestMapping("sys/post")
@Tag(name = "岗位管理")
@AllArgsConstructor
public class SysPostController {
    private final SysPostService sysPostService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
        @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true) ,
        @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true) ,
        @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY) ,
        @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY)
    })
    // @RequiresPermission("sys:post:page")
    public R<?> page(@Parameter(hidden = true)  @RequestMap DynMap params){
        PageData<SysPostDTO> page = sysPostService.page(params);
        return R.ok(page);
    }

    @GetMapping("list")
    @Operation(summary = "列表")
    public R<?> list(){
        DynMap params = new DynMap();
        //正常岗位列表
        params.put("status", "1");
        List<SysPostDTO> data = sysPostService.list(params);
        return R.ok(data);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("sys:post:info")
    public R<?> get(@PathVariable("id") Long id){
        SysPostDTO data = sysPostService.get(id);
        return R.ok(data);
    }

    @PostMapping
    @Operation(summary = "保存")
    @RequiresPermission("sys:post:save")
    public R<?> save(@RequestBody SysPostDTO dto){
        sysPostService.save(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    @RequiresPermission("sys:post:update")
    public R<?> update(@RequestBody SysPostDTO dto){
        sysPostService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @RequiresPermission("sys:post:delete")
    public R<?> delete(@RequestParam Long[] ids){
        //效验数据
        AssertUtils.isArrayEmpty(ids, "id");
        sysPostService.delete(ids);
        return R.ok();
    }
}