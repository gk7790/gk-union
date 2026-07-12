package com.gk.infra.mq.service;

import com.gk.infra.mq.entity.MqOutboxEntity;

import java.util.List;

public interface MqOutboxService {
    MqOutboxEntity createIfAbsent(MqOutboxEntity event);

    List<MqOutboxEntity> lockDueEvents(String eventType, int limit, String workerId);

    void markDone(Long id);

    void markRetry(Long id, String errorCode, String errorMsg);

    void markDead(Long id, String errorCode, String errorMsg);
}
