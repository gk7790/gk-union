package com.gk.quartz.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
public class QuartzStateRepairer {
    /**
     * Quartz 调度器名称，必须和 ScheduleConfig 中的 schedulerName / instanceName 保持一致。
     */
    private static final String SCHED_NAME = "GkScheduler";

    /**
     * 清理脏数据场景一：
     * QRTZ_FIRED_TRIGGERS 表表示“已经触发、正在执行”的任务记录。
     * 如果服务部署、宕机或强制停止时中断，可能残留已经失效的 fired trigger。
     * 这里专门删除“CRON trigger 存在，但 QRTZ_CRON_TRIGGERS 明细缺失”的 fired trigger，
     * 避免 Quartz 启动时读取到不完整状态。
     * 注意：RDS/Linux MySQL 通常区分表名大小写，项目 Quartz 表使用官方默认大写 QRTZ_ 前缀。
     */
    private static final String DELETE_FIRED_TRIGGERS_WITH_MISSING_CRON_DETAIL = """
            DELETE ft FROM QRTZ_FIRED_TRIGGERS ft
            JOIN QRTZ_TRIGGERS t
              ON t.SCHED_NAME = ft.SCHED_NAME
             AND t.TRIGGER_NAME = ft.TRIGGER_NAME
             AND t.TRIGGER_GROUP = ft.TRIGGER_GROUP
            LEFT JOIN QRTZ_CRON_TRIGGERS ct
              ON ct.SCHED_NAME = t.SCHED_NAME
             AND ct.TRIGGER_NAME = t.TRIGGER_NAME
             AND ct.TRIGGER_GROUP = t.TRIGGER_GROUP
            WHERE t.SCHED_NAME = ?
              AND t.TRIGGER_TYPE = 'CRON'
              AND ct.TRIGGER_NAME IS NULL
            """;

    /**
     * 清理脏数据场景二：
     * 删除缺少 QRTZ_CRON_TRIGGERS 明细的 CRON trigger。
     * 这种数据通常来自异常中断、手工改表或旧版本调度数据不完整。
     */
    private static final String DELETE_CRON_TRIGGERS_WITH_MISSING_CRON_DETAIL = """
            DELETE t FROM QRTZ_TRIGGERS t
            LEFT JOIN QRTZ_CRON_TRIGGERS ct
              ON ct.SCHED_NAME = t.SCHED_NAME
             AND ct.TRIGGER_NAME = t.TRIGGER_NAME
             AND ct.TRIGGER_GROUP = t.TRIGGER_GROUP
            WHERE t.SCHED_NAME = ?
              AND t.TRIGGER_TYPE = 'CRON'
              AND ct.TRIGGER_NAME IS NULL
            """;

    /**
     * 清理脏数据场景三：
     * 删除已经没有任何 trigger 关联的项目任务 Job。
     * 项目内动态任务 Job 名称使用 TASK_ 前缀，所以这里只清理 TASK_%，避免误删 Quartz 其他 Job。
     */
    private static final String DELETE_TASK_JOBS_WITHOUT_TRIGGERS = """
            DELETE jd FROM QRTZ_JOB_DETAILS jd
            LEFT JOIN QRTZ_TRIGGERS t
              ON t.SCHED_NAME = jd.SCHED_NAME
             AND t.JOB_NAME = jd.JOB_NAME
             AND t.JOB_GROUP = jd.JOB_GROUP
            WHERE jd.SCHED_NAME = ?
              AND jd.JOB_NAME LIKE 'TASK_%'
              AND t.TRIGGER_NAME IS NULL
            """;

    /**
     * 在 Quartz 正式启动前修复数据库中的异常调度状态。
     * 这个方法只清理确定不完整的调度记录，不会删除正常的任务配置。
     */
    public void repair(Connection connection) throws SQLException {
        int firedTriggerCount = executeUpdate(connection, DELETE_FIRED_TRIGGERS_WITH_MISSING_CRON_DETAIL);
        int triggerCount = executeUpdate(connection, DELETE_CRON_TRIGGERS_WITH_MISSING_CRON_DETAIL);
        int jobCount = executeUpdate(connection, DELETE_TASK_JOBS_WITHOUT_TRIGGERS);
        if (firedTriggerCount + triggerCount + jobCount > 0) {
            log.warn("Repaired dirty Quartz state: firedTriggers={}, triggers={}, jobs={}",
                    firedTriggerCount, triggerCount, jobCount);
        }
    }

    /**
     * 所有修复 SQL 都按同一个调度器名称过滤，避免误处理其他 scheduler 的数据。
     */
    private int executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SCHED_NAME);
            return statement.executeUpdate();
        }
    }
}
