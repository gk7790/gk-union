package com.gk.quartz.controller;

import com.gk.common.annotation.RequestMap;
import com.gk.common.annotation.RequiresPermission;
import com.gk.common.constant.Constant;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.common.tools.R;
import com.gk.quartz.dto.ScheduleJobLogDTO;
import com.gk.quartz.service.ScheduleJobLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务日志
 *
 * @author Lowen
 */
@RestController
@RequestMapping("/sys/scheduleLog")
@Tag(name = "定时任务日志")
@AllArgsConstructor
public class ScheduleJobLogController {
    private final ScheduleJobLogService scheduleJobLogService;

    @GetMapping("page")
    @Operation(summary = "分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", in = ParameterIn.QUERY, required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段", in = ParameterIn.QUERY),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)", in = ParameterIn.QUERY),
            @Parameter(name = "jobId", description = "jobId", in = ParameterIn.QUERY)
    })
    @RequiresPermission("sys:schedule:log")
    public R page(@Parameter(hidden = true) @RequestMap DataMap params) {
        PageData<ScheduleJobLogDTO> page = scheduleJobLogService.page(params);
        return R.ok(page);
    }

    @GetMapping("{id}")
    @Operation(summary = "信息")
    @RequiresPermission("sys:schedule:log")
    public R info(@PathVariable("id") Long id) {
        ScheduleJobLogDTO log = scheduleJobLogService.get(id);
        return R.ok(log);
    }
}
