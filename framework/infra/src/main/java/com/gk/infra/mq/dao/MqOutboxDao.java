package com.gk.infra.mq.dao;

import com.gk.common.core.dao.BaseDao;
import com.gk.infra.mq.entity.MqOutboxEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface MqOutboxDao extends BaseDao<MqOutboxEntity> {
    List<MqOutboxEntity> selectDueForConsume(@Param("eventType") String eventType,
                                             @Param("now") Instant now,
                                             @Param("limit") int limit,
                                             @Param("initStatus") String initStatus,
                                             @Param("failedStatus") String failedStatus,
                                             @Param("lockedStatus") String lockedStatus);

    int lockForConsume(@Param("id") Long id,
                       @Param("lockedBy") String lockedBy,
                       @Param("now") Instant now,
                       @Param("lockUntil") Instant lockUntil,
                       @Param("initStatus") String initStatus,
                       @Param("failedStatus") String failedStatus,
                       @Param("lockedStatus") String lockedStatus);

    int markConsumeDone(@Param("id") Long id,
                        @Param("lockedBy") String lockedBy,
                        @Param("now") Instant now,
                        @Param("doneStatus") String doneStatus,
                        @Param("lockedStatus") String lockedStatus);

    int markConsumeFailed(@Param("id") Long id,
                          @Param("lockedBy") String lockedBy,
                          @Param("nextRetryAt") Instant nextRetryAt,
                          @Param("errorCode") String errorCode,
                          @Param("errorMsg") String errorMsg,
                          @Param("failedStatus") String failedStatus,
                          @Param("lockedStatus") String lockedStatus);

    int markConsumeDead(@Param("id") Long id,
                        @Param("lockedBy") String lockedBy,
                        @Param("now") Instant now,
                        @Param("errorCode") String errorCode,
                        @Param("errorMsg") String errorMsg,
                        @Param("deadStatus") String deadStatus,
                        @Param("lockedStatus") String lockedStatus);
}
