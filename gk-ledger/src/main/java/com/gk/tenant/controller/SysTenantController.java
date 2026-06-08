package com.gk.tenant.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.exception.ErrorCode;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.tenant.dto.SysTenantDTO;
import com.gk.tenant.service.SysTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/sys/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService sysTenantService;

    /**
     * 分页
     */
    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true) ,
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true) ,
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY) ,
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY) ,
    })
    @RequiresPermission("sys:user:page")
    public R<?> page(@RequestMap DynMap params){
        PageData<SysTenantDTO> page = sysTenantService.page(params);
        return R.ok(page);
    }

    @GetMapping("dict")
    @Operation(summary = "字典")
    public R<?> dict(@RequestMap DynMap params){
        List<LabelDTO> list = sysTenantService.getDict(params);
        return R.ok(list);
    }


    @PostMapping
    @Operation(summary = "保存")
    public R<?> save(@RequestBody SysTenantDTO dto){
        //效验数据
        sysTenantService.save(dto);
        return R.ok();
    }

    @PutMapping("{id}")
    @Operation(summary = "修改")
    public R<?> update(@PathVariable("id") Long id, @RequestBody SysTenantDTO dto){
        //效验数据
        AssertUtils.isReserved(id);
        dto.setId(id);
        sysTenantService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    public R<?> delete(@RequestParam Long id){
        //效验数据
        AssertUtils.isReserved(id);
        sysTenantService.delete(id);
        return R.ok();
    }

}
