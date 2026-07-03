package com.gk.quartz.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
public class QuartzStateRepairer {
    private static final String SCHED_NAME = "GkScheduler";

    private static final String DELETE_FIRED_TRIGGERS_WITH_MISSING_CRON_DETAIL = """
            DELETE ft FROM qrtz_fired_triggers ft
            JOIN qrtz_triggers t
              ON t.SCHED_NAME = ft.SCHED_NAME
             AND t.TRIGGER_NAME = ft.TRIGGER_NAME
             AND t.TRIGGER_GROUP = ft.TRIGGER_GROUP
            LEFT JOIN qrtz_cron_triggers ct
              ON ct.SCHED_NAME = t.SCHED_NAME
             AND ct.TRIGGER_NAME = t.TRIGGER_NAME
             AND ct.TRIGGER_GROUP = t.TRIGGER_GROUP
            WHERE t.SCHED_NAME = ?
              AND t.TRIGGER_TYPE = 'CRON'
              AND ct.TRIGGER_NAME IS NULL
            """;

    private static final String DELETE_CRON_TRIGGERS_WITH_MISSING_CRON_DETAIL = """
            DELETE t FROM qrtz_triggers t
            LEFT JOIN qrtz_cron_triggers ct
              ON ct.SCHED_NAME = t.SCHED_NAME
             AND ct.TRIGGER_NAME = t.TRIGGER_NAME
             AND ct.TRIGGER_GROUP = t.TRIGGER_GROUP
            WHERE t.SCHED_NAME = ?
              AND t.TRIGGER_TYPE = 'CRON'
              AND ct.TRIGGER_NAME IS NULL
            """;

    private static final String DELETE_TASK_JOBS_WITHOUT_TRIGGERS = """
            DELETE jd FROM qrtz_job_details jd
            LEFT JOIN qrtz_triggers t
              ON t.SCHED_NAME = jd.SCHED_NAME
             AND t.JOB_NAME = jd.JOB_NAME
             AND t.JOB_GROUP = jd.JOB_GROUP
            WHERE jd.SCHED_NAME = ?
              AND jd.JOB_NAME LIKE 'TASK_%'
              AND t.TRIGGER_NAME IS NULL
            """;

    public void repair(Connection connection) throws SQLException {
        int firedTriggerCount = executeUpdate(connection, DELETE_FIRED_TRIGGERS_WITH_MISSING_CRON_DETAIL);
        int triggerCount = executeUpdate(connection, DELETE_CRON_TRIGGERS_WITH_MISSING_CRON_DETAIL);
        int jobCount = executeUpdate(connection, DELETE_TASK_JOBS_WITHOUT_TRIGGERS);
        if (firedTriggerCount + triggerCount + jobCount > 0) {
            log.warn("Repaired dirty Quartz state: firedTriggers={}, triggers={}, jobs={}",
                    firedTriggerCount, triggerCount, jobCount);
        }
    }

    private int executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SCHED_NAME);
            return statement.executeUpdate();
        }
    }
}
