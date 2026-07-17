package com.gk.quartz.utils;

import com.gk.common.constant.Constant;
import com.gk.common.exception.ExceptionUtils;
import com.gk.common.task.ITask;
import com.gk.common.task.TaskExecution;
import com.gk.common.task.TaskExecutions;
import com.gk.common.utils.ConvertUtils;
import com.gk.common.utils.SpringContextUtils;
import com.gk.quartz.entity.ScheduleJobEntity;
import com.gk.quartz.entity.ScheduleJobLogEntity;
import com.gk.quartz.service.ScheduleJobLogService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;


/**
 * 定时任务
 *
 * @author Lowen
 */
@DisallowConcurrentExecution
public class ScheduleJob extends QuartzJobBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
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

        TaskExecution execution = null;
        try {
            execution = TaskExecutions.start(scheduleJob.getBeanName());
            //执行任务
            logger.debug("任务准备执行，任务ID：{}", scheduleJob.getId());
            Object target = SpringContextUtils.getBean(scheduleJob.getBeanName());
            if (!(target instanceof ITask task)) {
                throw new IllegalStateException("Scheduled bean must implement ITask: " + scheduleJob.getBeanName());
            }
            String result = task.run(scheduleJob.getParams());

            //任务执行总时长
            long times = System.currentTimeMillis() - startTime;
            log.setTimes((int) times);
            //任务状态
            log.setStatus(Constant.SUCCESS);
            // 任务执行的结果,要求字符串
            log.setResult(execution.summary(result));

            logger.debug("任务执行完毕，任务ID：{}  总共耗时：{} 毫秒", scheduleJob.getId(), times);
        } catch (Exception e) {
            logger.debug("任务执行失败，任务ID：{}", scheduleJob.getId(), e);

            //任务执行总时长
            long times = System.currentTimeMillis() - startTime;
            log.setTimes((int) times);

            //任务状态
            log.setStatus(Constant.FAIL);
            if (execution != null) {
                log.setResult(execution.summary("FAILED"));
            }
            log.setError(ExceptionUtils.getErrorStackTrace(e));
            throw new JobExecutionException("Scheduled task execution failed: " + scheduleJob.getBeanName(), e);
        } finally {
            TaskExecutions.clear();
            //获取spring bean
            try {
                ScheduleJobLogService scheduleJobLogService = SpringContextUtils.getBean(ScheduleJobLogService.class);
                scheduleJobLogService.insert(log);
            } catch (Exception logException) {
                logger.error("Save schedule job log failed, jobId={}, beanName={}",
                        scheduleJob.getId(), scheduleJob.getBeanName(), logException);
            }
        }
    }
}
