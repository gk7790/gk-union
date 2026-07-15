package com.gk.quartz.init;

import com.gk.common.database.DatabaseProperties;
import com.gk.common.database.DatabaseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Component
public class QuartzStateRepairer {
    private static final String SCHED_NAME = "GkScheduler";

    private final DatabaseProperties databaseProperties;

    public QuartzStateRepairer(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    /**
     * 在 Quartz 正式启动前修复数据库中的异常调度状态。
     * 只清理确定不完整的调度记录，不删除正常任务配置。
     */
    public void repair(Connection connection) throws SQLException {
        QuartzSql quartzSql = QuartzSql.from(databaseProperties.getType());
        int firedTriggerCount = executeUpdate(connection, quartzSql.deleteFiredTriggersWithMissingCronDetail());
        int triggerCount = executeUpdate(connection, quartzSql.deleteCronTriggersWithMissingCronDetail());
        int jobCount = executeUpdate(connection, quartzSql.deleteTaskJobsWithoutTriggers());
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

    private record QuartzSql(QuartzNaming naming) {
        private static QuartzSql from(DatabaseType databaseType) {
            if (databaseType == DatabaseType.POSTGRESQL) {
                return new QuartzSql(QuartzNaming.postgresql());
            }
            return new QuartzSql(QuartzNaming.mysql());
        }

        private String deleteFiredTriggersWithMissingCronDetail() {
            return """
                    DELETE FROM %s
                    WHERE EXISTS (
                        SELECT 1
                        FROM %s t
                        LEFT JOIN %s ct
                          ON ct.%s = t.%s
                         AND ct.%s = t.%s
                         AND ct.%s = t.%s
                        WHERE t.%s = ?
                          AND t.%s = 'CRON'
                          AND ct.%s IS NULL
                          AND t.%s = %s.%s
                          AND t.%s = %s.%s
                          AND t.%s = %s.%s
                    )
                    """.formatted(
                    naming.firedTriggers(),
                    naming.triggers(),
                    naming.cronTriggers(),
                    naming.schedName(), naming.schedName(),
                    naming.triggerName(), naming.triggerName(),
                    naming.triggerGroup(), naming.triggerGroup(),
                    naming.schedName(),
                    naming.triggerType(),
                    naming.triggerName(),
                    naming.schedName(), naming.firedTriggers(), naming.schedName(),
                    naming.triggerName(), naming.firedTriggers(), naming.triggerName(),
                    naming.triggerGroup(), naming.firedTriggers(), naming.triggerGroup()
            );
        }

        private String deleteCronTriggersWithMissingCronDetail() {
            return """
                    DELETE FROM %s
                    WHERE %s = ?
                      AND %s = 'CRON'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM %s ct
                          WHERE ct.%s = %s.%s
                            AND ct.%s = %s.%s
                            AND ct.%s = %s.%s
                      )
                    """.formatted(
                    naming.triggers(),
                    naming.schedName(),
                    naming.triggerType(),
                    naming.cronTriggers(),
                    naming.schedName(), naming.triggers(), naming.schedName(),
                    naming.triggerName(), naming.triggers(), naming.triggerName(),
                    naming.triggerGroup(), naming.triggers(), naming.triggerGroup()
            );
        }

        private String deleteTaskJobsWithoutTriggers() {
            return """
                    DELETE FROM %s
                    WHERE %s = ?
                      AND %s LIKE 'TASK_%%'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM %s t
                          WHERE t.%s = %s.%s
                            AND t.%s = %s.%s
                            AND t.%s = %s.%s
                      )
                    """.formatted(
                    naming.jobDetails(),
                    naming.schedName(),
                    naming.jobName(),
                    naming.triggers(),
                    naming.schedName(), naming.jobDetails(), naming.schedName(),
                    naming.jobName(), naming.jobDetails(), naming.jobName(),
                    naming.jobGroup(), naming.jobDetails(), naming.jobGroup()
            );
        }
    }

    private record QuartzNaming(
            String firedTriggers,
            String triggers,
            String cronTriggers,
            String jobDetails,
            String schedName,
            String triggerName,
            String triggerGroup,
            String triggerType,
            String jobName,
            String jobGroup
    ) {
        private static QuartzNaming mysql() {
            return new QuartzNaming(
                    "QRTZ_FIRED_TRIGGERS",
                    "QRTZ_TRIGGERS",
                    "QRTZ_CRON_TRIGGERS",
                    "QRTZ_JOB_DETAILS",
                    "SCHED_NAME",
                    "TRIGGER_NAME",
                    "TRIGGER_GROUP",
                    "TRIGGER_TYPE",
                    "JOB_NAME",
                    "JOB_GROUP"
            );
        }

        private static QuartzNaming postgresql() {
            return new QuartzNaming(
                    "qrtz_fired_triggers",
                    "qrtz_triggers",
                    "qrtz_cron_triggers",
                    "qrtz_job_details",
                    "sched_name",
                    "trigger_name",
                    "trigger_group",
                    "trigger_type",
                    "job_name",
                    "job_group"
            );
        }

    }
}
