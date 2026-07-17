package com.gk.quartz.utils;

import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.infra.enums.StatusEnum;
import com.gk.quartz.entity.ScheduleJobEntity;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.NoRecordFoundException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 定时任务工具类
 *
 * @author Lowen
 */
public class ScheduleUtils {
    private final static String JOB_NAME = "TASK_";
    /**
     * 任务调度参数key
     */
    public static final String JOB_PARAM_KEY = "JOB_PARAM_KEY";

    /**
     * 获取触发器key
     */
    public static TriggerKey getTriggerKey(Long jobId) {
        return TriggerKey.triggerKey(JOB_NAME + jobId);
    }

    /**
     * 获取jobKey
     */
    public static JobKey getJobKey(Long jobId) {
        return JobKey.jobKey(JOB_NAME + jobId);
    }

    /**
     * 获取表达式触发器
     */
    public static CronTrigger getCronTrigger(Scheduler scheduler, Long jobId) {
        TriggerKey triggerKey = getTriggerKey(jobId);
        try {
            return (CronTrigger) scheduler.getTrigger(triggerKey);
        } catch (SchedulerException e) {
            if (isNoRecordFoundException(e)) {
                cleanScheduleJob(scheduler, jobId, e);
                return null;
            }
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, ScheduleJobEntity scheduleJob) {
        try {
            doCreateScheduleJob(scheduler, scheduleJob);
        } catch (SchedulerException e) {
            if (isDuplicateQuartzStateException(e)) {
                cleanScheduleJob(scheduler, scheduleJob.getId(), e);
                retryCreateScheduleJob(scheduler, scheduleJob, e);
                return;
            }
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    /**
     * 更新定时任务
     */
    public static void updateScheduleJob(Scheduler scheduler, ScheduleJobEntity scheduleJob) {
        try {
            TriggerKey triggerKey = getTriggerKey(scheduleJob.getId());

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();

            CronTrigger trigger = getCronTrigger(scheduler, scheduleJob.getId());
            if (trigger == null) {
                createScheduleJob(scheduler, scheduleJob);
                return;
            }

            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

            trigger.getJobDataMap().put(JOB_PARAM_KEY, scheduleJob);

            scheduler.rescheduleJob(triggerKey, trigger);

            if (Objects.equals(scheduleJob.getStatus(), StatusEnum.PAUSE.code())) {
                pauseJob(scheduler, scheduleJob.getId());
            }

        } catch (SchedulerException e) {
            if (isDuplicateQuartzStateException(e)) {
                cleanScheduleJob(scheduler, scheduleJob.getId(), e);
                createScheduleJob(scheduler, scheduleJob);
                return;
            }
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    private static void doCreateScheduleJob(Scheduler scheduler, ScheduleJobEntity scheduleJob) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(ScheduleJob.class).withIdentity(getJobKey(scheduleJob.getId())).build();

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCronExpression())
                .withMisfireHandlingInstructionDoNothing();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(scheduleJob.getId()))
                .withSchedule(scheduleBuilder)
                .build();

        jobDetail.getJobDataMap().put(JOB_PARAM_KEY, scheduleJob);

        scheduler.scheduleJob(jobDetail, trigger);

        if (Objects.equals(scheduleJob.getStatus(), StatusEnum.PAUSE.code())) {
            pauseJob(scheduler, scheduleJob.getId());
        }
    }

    private static void retryCreateScheduleJob(Scheduler scheduler, ScheduleJobEntity scheduleJob, SchedulerException original) {
        try {
            doCreateScheduleJob(scheduler, scheduleJob);
        } catch (SchedulerException e) {
            e.addSuppressed(original);
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    private static void cleanScheduleJob(Scheduler scheduler, Long jobId, SchedulerException cause) {
        try {
            scheduler.deleteJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            e.addSuppressed(cause);
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    private static boolean isNoRecordFoundException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof NoRecordFoundException) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private static boolean isDuplicateQuartzStateException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (throwable instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            String message = throwable.getMessage();
            if (message != null && message.contains("Duplicate entry") && message.toLowerCase().contains("qrtz_")) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    /**
     * 立即执行任务
     */
    public static void run(Scheduler scheduler, ScheduleJobEntity scheduleJob) {
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(JOB_PARAM_KEY, scheduleJob);
            scheduler.triggerJob(getJobKey(scheduleJob.getId()), dataMap);
        } catch (SchedulerException e) {
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    /**
     * 暂停任务
     */
    public static void pauseJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.pauseJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.resumeJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    /**
     * 删除定时任务
     */
    public static void deleteScheduleJob(Scheduler scheduler, Long jobId) {
        try {
            scheduler.deleteJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }
}
