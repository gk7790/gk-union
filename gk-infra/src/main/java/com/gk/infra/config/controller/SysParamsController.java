package com.gk.infra.config.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.config.dto.SysParamsDTO;
import com.gk.infra.config.service.SysParamsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 参数管理
 *
 * @author Lowen
 * @since 1.0.0
 */
@RestController
@RequestMapping("sys/params")
@Tag(name = "参数管理")
@AllArgsConstructor
public class SysParamsController {
    private final SysParamsService sysParamsService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
        @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true) ,
        @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true) ,
        @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY) ,
        @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY) ,
        @Parameter(name = "paramCode", description = "参数编码", in = ParameterIn.QUERY)
    })
    public R<?> page(@Parameter(hidden = true) @RequestMap DataMap params){
        PageData<SysParamsDTO> page = sysParamsService.page(params);

        return R.ok(page);
    }

    @PostMapping
    @Operation(summary = "保存")
    public R<?> save(@RequestBody SysParamsDTO dto){
        //效验数据
        sysParamsService.save(dto);

        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    public R<?> update(@RequestBody SysParamsDTO dto){
        //效验数据
        sysParamsService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    public R<?> delete(@RequestParam Long[] ids){
        //效验数据
        AssertUtils.isArrayEmpty(ids, "id");

        sysParamsService.delete(ids);

        return R.ok();
    }

}