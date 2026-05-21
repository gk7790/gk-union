package com.gk.quartz.service;


import com.gk.common.core.service.BaseService;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.quartz.dto.ScheduleJobLogDTO;
import com.gk.quartz.entity.ScheduleJobLogEntity;

import java.util.Map;

/**
 * 定时任务日志
 *
 * @author Lowen
 */
public interface ScheduleJobLogService extends BaseService<ScheduleJobLogEntity> {

	PageData<ScheduleJobLogDTO> page(DataMap params);

	ScheduleJobLogDTO get(Long id);
}
