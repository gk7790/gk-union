package com.gk.infra.mq.service;

import com.gk.infra.mq.entity.MqOutboxEntity;

import java.util.List;

public interface MqOutboxService {
    MqOutboxEntity createIfAbsent(MqOutboxEntity event);

    List<MqOutboxEntity> lockDueEvents(String eventType, int limit, String workerId);

    default void markDone(MqOutboxEntity event) {
        markDone(event.getId(), event.getLockedBy());
    }

    void markDone(Long id, String workerId);

    default void markRetry(MqOutboxEntity event, String errorCode, String errorMsg) {
        markRetry(event.getId(), event.getLockedBy(), errorCode, errorMsg);
    }

    void markRetry(Long id, String workerId, String errorCode, String errorMsg);

    default void markDead(MqOutboxEntity event, String errorCode, String errorMsg) {
        markDead(event.getId(), event.getLockedBy(), errorCode, errorMsg);
    }

    void markDead(Long id, String workerId, String errorCode, String errorMsg);
}
