package com.gk.platform.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.context.ReqContextHolder;
import com.gk.common.dto.LabelDTO;
import com.gk.common.page.PageData;
import com.gk.common.tools.DynMap;
import com.gk.common.tools.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.config.service.SysParamsService;
import com.gk.platform.dto.SysI18nDTO;
import com.gk.platform.service.SysI18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
* 系统-国际化
*
* @author Lowen lowen@gmail.com
* @since 3.0 2026-05-23
*/
@RestController
@RequestMapping("sys/i18n")
@Tag(name = "系统-国际化")
@RequiredArgsConstructor
public class SysI18nController {
    private final SysI18nService sysI18nService;
    private final SysParamsService sysParamsService;


    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
        @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true, ref="int") ,
        @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY,required = true, ref="int") ,
        @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY, ref="String") ,
        @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY, ref="String")
    })
    public R<?> page(@Parameter(hidden = true) @RequestMap DynMap params){
        params.put("lang", ReqContextHolder.getLang());
        Long tenantId = ReqContextHolder.getTenantId();
        params.put("tenantId", tenantId);
        PageData<SysI18nDTO> page = sysI18nService.getPage(params);
        for (SysI18nDTO item : page.getItems()) {
            item.setHasChild(Boolean.TRUE);
        }
        return R.ok(page);
    }

    /**
     * 系统语言参数
     */
    @GetMapping("list")
    public R<?> list(@Parameter(hidden = true) @RequestMap DynMap params){
        List<SysI18nDTO> list = sysI18nService.getList(params);
        for (SysI18nDTO item : list) {
            item.setHasChild(Boolean.FALSE);
        }
        return R.ok(list);
    }

    /**
     * 系统语言参数
     */
    @GetMapping("langs")
    public R<?> listLang(){
        List<LabelDTO> list = sysParamsService.getValueList(Constant.SYS_I18N_PARAMS_KEY, LabelDTO.class);
        return R.ok(list);
    }

    /**
     * 系统语言参数
     */
    @GetMapping("biz-type")
    public R<?> bizType(){
        List<LabelDTO> list = sysParamsService.getValueList(Constant.SYS_I18N_TYPE_KEY, LabelDTO.class);
        return R.ok(list);
    }

    @PostMapping("add")
    @Operation(summary = "保存")
    public R<?> insert(@RequestBody SysI18nDTO dto){
        sysI18nService.addKeyValue(dto);
        return R.ok();
    }

    @PostMapping
    @Operation(summary = "保存")
    public R<?> save(@RequestBody SysI18nDTO dto){
        if (dto.getId() != null) {
            sysI18nService.update(dto);
        } else {
            sysI18nService.save(dto);
        }
        return R.ok();
    }


    @DeleteMapping
    @Operation(summary = "删除")
    public R<?> delete(@RequestParam Long id){
        //效验数据
        AssertUtils.isNull(id, "id");
        sysI18nService.deleteById(id);
        return R.ok();
    }
}