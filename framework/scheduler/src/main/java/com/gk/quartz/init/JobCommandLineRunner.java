package com.gk.quartz.init;

import com.gk.common.database.DatabaseProperties;
import com.gk.common.database.DatabaseType;
import com.gk.common.exception.ErrorCode;
import com.gk.common.exception.GkException;
import com.gk.quartz.dao.ScheduleJobDao;
import com.gk.quartz.entity.ScheduleJobEntity;
import com.gk.quartz.utils.ScheduleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 初始化定时任务数据
 *
 * @author Lowen
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCommandLineRunner implements CommandLineRunner {
    private static final String STARTUP_LOCK_NAME = "gk:scheduler:init";
    private static final int STARTUP_LOCK_TIMEOUT_SECONDS = 30;
    private static final String MYSQL_GET_LOCK_SQL = "SELECT GET_LOCK(?, ?)";
    private static final String MYSQL_RELEASE_LOCK_SQL = "SELECT RELEASE_LOCK(?)";
    private static final String POSTGRESQL_TRY_LOCK_SQL = "SELECT pg_try_advisory_lock(hashtext(?))";
    private static final String POSTGRESQL_RELEASE_LOCK_SQL = "SELECT pg_advisory_unlock(hashtext(?))";

    private final Scheduler scheduler;
    private final ScheduleJobDao scheduleJobDao;
    private final DataSource dataSource;
    private final QuartzStateRepairer quartzStateRepairer;
    private final DatabaseProperties databaseProperties;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            boolean locked = false;
            try {
                locked = acquireStartupLock(connection);
                if (!locked) {
                    log.warn("Skip schedule job initialization because startup lock was not acquired: {}", STARTUP_LOCK_NAME);
                    return;
                }
                quartzStateRepairer.repair(connection);
                syncScheduleJobs();
            } finally {
                if (locked) {
                    releaseStartupLock(connection);
                }
            }
        } catch (SQLException e) {
            throw new GkException(ErrorCode.JOB_ERROR, e);
        }
    }

    private boolean acquireStartupLock(Connection connection) throws SQLException {
        if (databaseProperties.getType() == DatabaseType.POSTGRESQL) {
            return acquirePostgresqlStartupLock(connection);
        }
        try (PreparedStatement statement = connection.prepareStatement(MYSQL_GET_LOCK_SQL)) {
            statement.setString(1, STARTUP_LOCK_NAME);
            statement.setInt(2, STARTUP_LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean acquirePostgresqlStartupLock(Connection connection) throws SQLException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(STARTUP_LOCK_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadlineNanos) {
            if (tryAcquirePostgresqlStartupLock(connection)) {
                return true;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean tryAcquirePostgresqlStartupLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(POSTGRESQL_TRY_LOCK_SQL)) {
            statement.setString(1, STARTUP_LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void releaseStartupLock(Connection connection) {
        try {
            String releaseLockSql = databaseProperties.getType() == DatabaseType.POSTGRESQL
                    ? POSTGRESQL_RELEASE_LOCK_SQL
                    : MYSQL_RELEASE_LOCK_SQL;
            try (PreparedStatement statement = connection.prepareStatement(releaseLockSql)) {
                statement.setString(1, STARTUP_LOCK_NAME);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next() && !isLockReleased(resultSet)) {
                        log.warn("Schedule job startup lock was not released by this connection: {}", STARTUP_LOCK_NAME);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to release schedule job startup lock: {}", STARTUP_LOCK_NAME, e);
        }
    }

    private boolean isLockReleased(ResultSet resultSet) throws SQLException {
        if (databaseProperties.getType() == DatabaseType.POSTGRESQL) {
            return resultSet.getBoolean(1);
        }
        return resultSet.getInt(1) == 1;
    }

    private void syncScheduleJobs() {
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
