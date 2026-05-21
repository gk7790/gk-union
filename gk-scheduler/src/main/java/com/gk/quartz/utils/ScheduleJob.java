package com.gk.quartz.utils;

import cn.hutool.core.util.ObjectUtil;
import com.gk.common.constant.Constant;
import com.gk.common.exception.ExceptionUtils;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.SpringContextUtils;
import com.gk.quartz.entity.ScheduleJobEntity;
import com.gk.quartz.entity.ScheduleJobLogEntity;
import com.gk.quartz.service.ScheduleJobLogService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.lang.reflect.Method;
import java.time.LocalDateTime;


/**
 * 定时任务
 *
 * @author Lowen
 */
public class ScheduleJob extends QuartzJobBean {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void executeInternal(JobExecutionContext context) {
        Object job = context.getMergedJobDataMap().get(ScheduleUtils.JOB_PARAM_KEY);
        ScheduleJobEntity scheduleJob = ConvertUtils.sourceToTarget(job, ScheduleJobEntity.class);

        //数据库保存执行记录
        ScheduleJobLogEntity log = new ScheduleJobLogEntity();
        log.setJobId(scheduleJob.getId());
        log.setBeanName(scheduleJob.getBeanName());
        log.setParams(scheduleJob.getParams());
        log.setCreatedAt(LocalDateTime.now());

        //任务开始时间
        long startTime = System.currentTimeMillis();

        try {
            //执行任务
            logger.info("任务准备执行，任务ID：{}", scheduleJob.getId());
            Object target = SpringContextUtils.getBean(scheduleJob.getBeanName());
            Method method = target.getClass().getDeclaredMethod("run", String.class);
            Object result = method.invoke(target, scheduleJob.getParams());

            //任务执行总时长
            long times = System.currentTimeMillis() - startTime;
            log.setTimes((int) times);
            //任务状态
            log.setStatus(Constant.SUCCESS);
            // 任务执行的结果,要求字符串
            log.setResult(ObjectUtil.toString(result));

            logger.info("任务执行完毕，任务ID：{}  总共耗时：{} 毫秒", scheduleJob.getId(), times);
        } catch (Exception e) {
            logger.error("任务执行失败，任务ID：{}", scheduleJob.getId(), e);

            //任务执行总时长
            long times = System.currentTimeMillis() - startTime;
            log.setTimes((int) times);

            //任务状态
            log.setStatus(Constant.FAIL);
            log.setError(ExceptionUtils.getErrorStackTrace(e));
        } finally {
            //获取spring bean
            ScheduleJobLogService scheduleJobLogService = SpringContextUtils.getBean(ScheduleJobLogService.class);
            scheduleJobLogService.insert(log);
        }
    }
}