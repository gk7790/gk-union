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
                                             @Param("limit") int limit);

    int lockForConsume(@Param("id") Long id,
                       @Param("lockedBy") String lockedBy,
                       @Param("now") Instant now,
                       @Param("lockUntil") Instant lockUntil);

    int markConsumeDone(@Param("id") Long id,
                        @Param("now") Instant now);

    int markConsumeFailed(@Param("id") Long id,
                          @Param("nextRetryAt") Instant nextRetryAt,
                          @Param("errorCode") String errorCode,
                          @Param("errorMsg") String errorMsg);

    int markConsumeDead(@Param("id") Long id,
                        @Param("now") Instant now,
                        @Param("errorCode") String errorCode,
                        @Param("errorMsg") String errorMsg);
}
