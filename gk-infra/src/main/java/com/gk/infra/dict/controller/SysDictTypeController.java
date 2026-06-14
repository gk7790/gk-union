package com.gk.infra.dict.controller;


import com.gk.common.annotation.RequestMap;
import com.gk.common.constant.Constant;
import com.gk.common.dto.LabelDTO;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.common.model.R;
import com.gk.common.validator.AssertUtils;
import com.gk.infra.dict.dto.SysDictTypeDTO;
import com.gk.infra.dict.entity.DictData;
import com.gk.infra.dict.entity.DictType;
import com.gk.infra.dict.service.SysDictTypeService;
import com.gk.infra.enumdict.EnumDictProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典类型
 *
 * @author Lowen
 */
@RestController
@RequestMapping("sys/dict/type")
@Tag(name="字典类型")
@AllArgsConstructor
public class SysDictTypeController {
    private final SysDictTypeService sysDictTypeService;
    private final EnumDictProvider enumDictProvider;

    @GetMapping("page")
    @Operation(summary = "字典类型")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "dictType", description = "字典类型", in = ParameterIn.QUERY),
            @Parameter(name = "dictName", description = "字典名称", in = ParameterIn.QUERY)
    })
    @PreAuthorize("hasAuthority('sys:dict:page')")
    public R<PageData<SysDictTypeDTO>> page(@Parameter(hidden = true) @RequestMap DynMap params){
        //字典类型
        PageData<SysDictTypeDTO> page = sysDictTypeService.page(params);
        return R.ok(page);
    }

    @GetMapping("{dictType}")
    public R<?> get(@PathVariable("dictType") String dictType){
        List<LabelDTO> list = sysDictTypeService.getDictList(dictType);
        List<LabelDTO> enumList = enumDictProvider.get(dictType);
        if (!enumList.isEmpty()) {
            list = new ArrayList<>(list);
            list.addAll(enumList);
        }
        return R.ok(list);
    }

    @GetMapping("dict")
    public R<?> dict(){
        List<DictType> dictTypeList = sysDictTypeService.getDictTypeList();
        return R.ok(dictTypeList.stream().map(e-> new LabelDTO(e.getId(), e.getDictType())).toList());
    }

    @PostMapping
    @Operation(summary = "保存")
    @PreAuthorize("hasAuthority('sys:dict:save')")
    public R<?> save(@RequestBody SysDictTypeDTO dto){
        sysDictTypeService.save(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    @PreAuthorize("hasAuthority('sys:dict:update')")
    public R<?> update(@RequestBody SysDictTypeDTO dto){
        sysDictTypeService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @PreAuthorize("hasAuthority('sys:dict:delete')")
    public R<?> delete(@RequestParam Long[] ids){
        //效验数据
        AssertUtils.isArrayEmpty(ids, "id");

        sysDictTypeService.delete(ids);

        return R.ok();
    }

    @GetMapping("all")
    @Operation(summary = "所有字典数据")
    public R<List<DictType>> all(){
        List<DictType> list = sysDictTypeService.getAllList();
        Map<String, DictType> dictMap = new LinkedHashMap<>();
        for (DictType item : list) {
            dictMap.put(item.getDictType(), item);
        }
        enumDictProvider.list(null).forEach((dictType, dataList) -> {
            DictType item = dictMap.computeIfAbsent(dictType, key -> {
                DictType dict = new DictType();
                dict.setDictType(key);
                return dict;
            });
            item.getDataList().addAll(dataList.stream().map(this::toDictData).toList());
        });
        return R.ok(dictMap.values().stream().toList());
    }

    private DictData toDictData(LabelDTO label) {
        DictData data = new DictData();
        data.setDictLabel(label.getLabel());
        data.setDictValue(label.getValue() == null ? null : String.valueOf(label.getValue()));
        data.setAttrType(label.getAttrType());
        data.setI18nKey(label.getI18nKey());
        return data;
    }

}
