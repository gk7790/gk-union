package com.gk.quartz.service;


import com.gk.common.core.service.BaseService;
import com.gk.common.model.PageData;
import com.gk.common.model.DynMap;
import com.gk.quartz.dto.ScheduleJobLogDTO;
import com.gk.quartz.entity.ScheduleJobLogEntity;

/**
 * 定时任务日志
 *
 * @author Lowen
 */
public interface ScheduleJobLogService extends BaseService<ScheduleJobLogEntity> {

	PageData<ScheduleJobLogDTO> page(DynMap params);

	ScheduleJobLogDTO get(Long id);
}
