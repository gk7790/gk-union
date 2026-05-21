package com.gk.quartz.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gk.common.constant.Constant;
import com.gk.common.core.service.impl.BaseServiceImpl;
import com.gk.common.page.PageData;
import com.gk.common.tools.DataMap;
import com.gk.common.utils.ConvertUtils;
import com.gk.quartz.dao.ScheduleJobLogDao;
import com.gk.quartz.dto.ScheduleJobLogDTO;
import com.gk.quartz.entity.ScheduleJobLogEntity;
import com.gk.quartz.service.ScheduleJobLogService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleJobLogServiceImpl extends BaseServiceImpl<ScheduleJobLogDao, ScheduleJobLogEntity> implements ScheduleJobLogService {

    @Override
	public PageData<ScheduleJobLogDTO> page(DataMap params) {
		IPage<ScheduleJobLogEntity> page = baseDao.selectPage(
			getPage(params, Constant.CREATED_AT, false),
			getWrapper(params)
		);
		return getPageData(page, ScheduleJobLogDTO.class);
	}

	private QueryWrapper<ScheduleJobLogEntity> getWrapper(DataMap params){
		String jobId = MapUtil.getStr(params, "jobId");
		String beanName = MapUtil.getStr(params, "beanName");
		QueryWrapper<ScheduleJobLogEntity> wrapper = new QueryWrapper<>();
		wrapper.eq(StrUtil.isNotBlank(jobId), "job_id", jobId);
		wrapper.eq(StrUtil.isNotBlank(beanName), "bean_name", beanName);

		return wrapper;
	}

	@Override
	public ScheduleJobLogDTO get(Long id) {
		ScheduleJobLogEntity entity = baseDao.selectById(id);

		return ConvertUtils.sourceToTarget(entity, ScheduleJobLogDTO.class);
	}

}