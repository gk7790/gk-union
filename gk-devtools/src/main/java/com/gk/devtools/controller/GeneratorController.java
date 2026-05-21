package com.gk.devtools.controller;

import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.devtools.config.DataSourceInfo;
import com.gk.devtools.entity.MenuEntity;
import com.gk.devtools.entity.TableFieldEntity;
import com.gk.devtools.entity.TableInfoEntity;
import com.gk.devtools.service.GeneratorService;
import com.gk.devtools.service.TableFieldService;
import com.gk.devtools.service.TableInfoService;
import com.gk.devtools.utils.DbUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 代码生成
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools")
@Tag(name = "开发工具-表结构")
@RequiredArgsConstructor
public class GeneratorController {
    private final GeneratorService generatorService;
    private final TableInfoService tableInfoService;
    private final TableFieldService tableFieldService;

    @GetMapping("table/page")
    public R<?> pageTable(@RequestParam Map<String, Object> params){
        PageData<TableInfoEntity> page = tableInfoService.page(params);
        return R.ok(page);
    }

    @GetMapping("table/{id}")
    public R<?> getTable(@PathVariable("id") Long id){
        TableInfoEntity table = tableInfoService.selectById(id);
        List<TableFieldEntity> fieldList = tableFieldService.getByTableName(table.getTableName());
        table.setFields(fieldList);
        return R.ok(fieldList);
    }

    @PutMapping("table")
    public R<?> updateTable(@RequestBody TableInfoEntity tableInfo){
        tableInfoService.updateById(tableInfo);
        return R.ok();
    }

    @DeleteMapping("table")
    public R<?> deleteTable(@RequestParam Long[] ids){
        tableInfoService.deleteBatchIds(ids);
        return R.ok();
    }

    /**
     * 获取数据源中所有表
     */
    @GetMapping("datasource/table/list/{id}")
    public R<?> getDataSourceTableList(@PathVariable("id") Long id){
        try {
            //初始化配置信息
            DataSourceInfo info = generatorService.getDataSourceInfo(id);
            List<TableInfoEntity> tableInfoList = DbUtils.getTablesInfoList(info);
            return R.ok(tableInfoList);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("数据源配置错误，请检查数据源配置！");
        }
    }

    /**
     * 导入数据源中的表
     */
    @PostMapping("datasource/table")
    public R<?> datasourceTable(@RequestBody TableInfoEntity tableInfo) {
        generatorService.datasourceTable(tableInfo);
        return R.ok();
    }

    /**
     * 更新列数据
     */
    @PutMapping("table/field/{tableId}")
    public R<?> updateTableField(@PathVariable("tableId") Long tableId, @RequestBody List<TableFieldEntity> tableFieldList) {
        generatorService.updateTableField(tableId, tableFieldList);
        return R.ok();
    }

    /**
     * 生成代码
     */
    @PostMapping("generator")
    public R<?> generator(@RequestBody TableInfoEntity tableInfo) {
        //保存表信息
        tableInfoService.updateById(tableInfo);

        List<TableFieldEntity> fieldList = tableFieldService.getByTableName(tableInfo.getTableName());
        tableInfo.setFields(fieldList);

        //生成代码
        generatorService.generatorCode(tableInfo);

        return R.ok();
    }

    /**
     * 创建菜单
     */
    @PostMapping("menu")
    public R<?> menu(@RequestBody MenuEntity menu) {
        //创建菜单
        generatorService.generatorMenu(menu);

        return R.ok();
    }
}