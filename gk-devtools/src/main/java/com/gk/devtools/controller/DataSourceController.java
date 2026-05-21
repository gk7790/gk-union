package com.gk.devtools.controller;

import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.devtools.config.DataSourceInfo;
import com.gk.devtools.entity.DataSourceEntity;
import com.gk.devtools.service.DataSourceService;
import com.gk.devtools.utils.DbUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据源管理
 *
 * @author Lowen
 */
@RestController
@RequestMapping("devtools/datasource")
@Tag(name = "开发工具-数据源")
@RequiredArgsConstructor
public class DataSourceController {
    private final DataSourceService datasourceService;

    @GetMapping("page")
    @Operation(summary = "数据源(分页)")
    public R<?> page(@RequestParam Map<String, Object> params){
        PageData<DataSourceEntity> page = datasourceService.page(params);
        return R.ok(page);
    }

    @GetMapping("list")
    @Operation(summary = "数据源-列表")
    public R<?> list(){
        List<DataSourceEntity> list = datasourceService.list();
        return R.ok(list);
    }

    @GetMapping("{id}")
    @Operation(summary = "数据源-详情")
    public R<?> get(@PathVariable("id") Long id){
        DataSourceEntity data = datasourceService.selectById(id);

        return R.ok(data);
    }

    @GetMapping("test/{id}")
    @Operation(summary = "数据源-连接测试")
    public R<?> test(@PathVariable("id") Long id){
        try {
            DataSourceEntity entity = datasourceService.selectById(id);
            DbUtils.getConnection(new DataSourceInfo(entity));
            return R.ok("连接成功");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("连接失败，请检查配置信息");
        }
    }

    @PostMapping
    @Operation(summary = "数据源-添加")
    public R<?> save(@RequestBody DataSourceEntity entity){
        datasourceService.insert(entity);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "数据源-修改")
    public R<?> update(@RequestBody DataSourceEntity entity){
        datasourceService.updateById(entity);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "数据源-删除")
    public R<?> delete(@RequestBody Long[] ids){
        datasourceService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }
}