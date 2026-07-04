package com.gk.quartz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz 定时任务配置。
 *
 * <p>项目使用数据库持久化 Quartz 状态，因此服务重启、重新部署或多实例运行时，
 * 都会通过同一套 qrtz_ 表记录 Job、Trigger 和执行状态。</p>
 *
 * @author Lowen
 */
@Configuration
public class ScheduleConfig {

    @Value("${gk.quartz.thread-count:8}")
    private int quartzThreadCount;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);

        Properties prop = new Properties();

        // Quartz 调度器名称，需要和 QuartzStateRepairer 中的 SCHED_NAME 保持一致。
        prop.put("org.quartz.scheduler.instanceName", "GkScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");

        // Quartz 线程池配置，线程数通过 gk.quartz.thread-count 控制。
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", String.valueOf(quartzThreadCount));
        prop.put("org.quartz.threadPool.threadPriority", "5");

        // 使用 Spring 管理的数据源，将 Quartz 状态持久化到数据库。
        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");

        // 开启集群模式，避免多实例部署时重复调度同一个任务。
        prop.put("org.quartz.jobStore.isClustered", "true");
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        // 超过该时间未触发的任务会被 Quartz 识别为 misfire。
        prop.put("org.quartz.jobStore.misfireThreshold", "12000");

        // RDS/Linux MySQL 通常区分表名大小写，数据库脚本中 Quartz 表使用小写 qrtz_ 前缀。
        prop.put("org.quartz.jobStore.tablePrefix", "qrtz_");

        // PostgreSQL 数据库需要打开下面配置。
        //prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");

        factory.setQuartzProperties(prop);
        factory.setSchedulerName("GkScheduler");

        // 延时启动，等待 Spring 容器和数据源初始化完成。
        factory.setStartupDelay(10);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");

        // 启动时覆盖已存在的 Job 定义，避免代码更新后还沿用数据库中的旧 Job 配置。
        factory.setOverwriteExistingJobs(true);

        // 自动启动 Quartz 调度器。
        factory.setAutoStartup(true);
        return factory;
    }
}
