package com.gk.quartz.controller;


import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.page.PageData;
import com.gk.common.tools.R;
import com.gk.quartz.dto.ScheduleJobDTO;
import com.gk.quartz.service.ScheduleJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 定时任务
 *
 * @author Lowen
 */
@RestController
@RequestMapping("/sys/schedule")
@Tag(name = "定时任务")
@AllArgsConstructor
public class ScheduleJobController {
    private final ScheduleJobService scheduleJobService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "beanName", description = "beanName", in = ParameterIn.QUERY)
    })
    @RequiresPermission("sys:schedule:page")
    public R page(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        PageData<ScheduleJobDTO> page = scheduleJobService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("sys:schedule:info")
    public R info(@PathVariable("id") Long id) {
        ScheduleJobDTO schedule = scheduleJobService.get(id);
        return R.ok(schedule);
    }

    @PostMapping
    @Operation(summary = "保存")
    @RequiresPermission("sys:schedule:save")
    public R<?> save(@RequestBody ScheduleJobDTO dto) {
        if (StringUtils.isNotBlank(dto.getParams())) {
            dto.setParams(StringEscapeUtils.escapeHtml4(dto.getParams()));
        }
        scheduleJobService.save(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改")
    @RequiresPermission("sys:schedule:update")
    public R update(@RequestBody ScheduleJobDTO dto) {
        if (StringUtils.isNotBlank(dto.getParams())) {
            dto.setParams(StringEscapeUtils.escapeHtml4(dto.getParams()));
        }
        scheduleJobService.update(dto);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "删除")
    @RequiresPermission("sys:schedule:delete")
    public R delete(@RequestParam Long[] ids) {
        scheduleJobService.deleteBatch(ids);
        return R.ok();
    }

    @PutMapping("/run")
    @Operation(summary = "立即执行")
    @RequiresPermission("sys:schedule:run")
    public R run(@RequestBody Long[] ids) {
        scheduleJobService.run(ids);
        return R.ok();
    }

    @PutMapping("/pause")
    @Operation(summary = "暂停")
    @RequiresPermission("sys:schedule:pause")
    public R pause(@RequestBody Long[] ids) {
        scheduleJobService.pause(ids);
        return R.ok();
    }

    @PutMapping("/resume")
    @Operation(summary = "恢复")
    @RequiresPermission("sys:schedule:resume")
    public R resume(@RequestBody Long[] ids) {
        scheduleJobService.resume(ids);
        return R.ok();
    }

}
