package com.gk.quartz.init;

import com.gk.quartz.dao.ScheduleJobDao;
import com.gk.quartz.entity.ScheduleJobEntity;
import com.gk.quartz.utils.ScheduleUtils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化定时任务数据
 *
 * @author Lowen
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCommandLineRunner implements CommandLineRunner {
    private final Scheduler scheduler;
    private final ScheduleJobDao scheduleJobDao;

    @Override
    public void run(String... args) {
        List<ScheduleJobEntity> scheduleJobList = scheduleJobDao.selectList(null);
        for (ScheduleJobEntity scheduleJob : scheduleJobList) {
            CronTrigger cronTrigger = ScheduleUtils.getCronTrigger(scheduler, scheduleJob.getId());
            //如果不存在，则创建
            if (cronTrigger == null) {
                ScheduleUtils.createScheduleJob(scheduler, scheduleJob);
            } else {
                ScheduleUtils.updateScheduleJob(scheduler, scheduleJob);
            }
        }
    }

}