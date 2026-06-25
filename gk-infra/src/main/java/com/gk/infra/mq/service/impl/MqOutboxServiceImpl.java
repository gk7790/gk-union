package com.gk.infra.mq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.infra.mq.dao.MqOutboxDao;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.service.MqOutboxService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MqOutboxServiceImpl implements MqOutboxService {
    private static final int DEFAULT_LOCK_SECONDS = 120;
    private static final int MAX_ERROR_CODE_LENGTH = 64;
    private static final int MAX_ERROR_MSG_LENGTH = 1024;

    private final MqOutboxDao mqOutboxDao;

    @Override
    public MqOutboxEntity createIfAbsent(MqOutboxEntity event) {
        try {
            mqOutboxDao.insert(event);
            return event;
        } catch (DuplicateKeyException ex) {
            return mqOutboxDao.selectOne(new QueryWrapper<MqOutboxEntity>()
                    .eq("tenant_id", event.getTenantId())
                    .eq("biz_type", event.getBizType())
                    .eq("biz_no", event.getBizNo())
                    .eq("event_type", event.getEventType())
                    .last("limit 1"));
        }
    }

    @Override
    public List<MqOutboxEntity> lockDueEvents(String eventType, int limit, String workerId) {
        Instant now = Instant.now();
        String lockOwner = StringUtils.defaultIfBlank(workerId, defaultWorkerId());
        List<MqOutboxEntity> candidates = mqOutboxDao.selectDueForConsume(eventType, now, limit);
        List<MqOutboxEntity> locked = new ArrayList<>();
        for (MqOutboxEntity candidate : candidates) {
            int updated = mqOutboxDao.lockForConsume(
                    candidate.getId(),
                    lockOwner,
                    now,
                    now.plusSeconds(DEFAULT_LOCK_SECONDS)
            );
            if (updated == 1) {
                candidate.setConsumeStatus("LOCKED");
                candidate.setLockedBy(lockOwner);
                locked.add(candidate);
            }
        }
        return locked;
    }

    @Override
    public void markDone(Long id) {
        mqOutboxDao.markConsumeDone(id, Instant.now());
    }

    @Override
    public void markRetry(Long id, String errorCode, String errorMsg) {
        Instant nextRetryAt = Instant.now().plusSeconds(30);
        int updated = mqOutboxDao.markConsumeFailed(
                id,
                nextRetryAt,
                StringUtils.left(errorCode, MAX_ERROR_CODE_LENGTH),
                StringUtils.left(errorMsg, MAX_ERROR_MSG_LENGTH)
        );
        if (updated != 1) {
            markDead(id, errorCode, errorMsg);
        }
    }

    @Override
    public void markDead(Long id, String errorCode, String errorMsg) {
        mqOutboxDao.markConsumeDead(
                id,
                Instant.now(),
                StringUtils.left(errorCode, MAX_ERROR_CODE_LENGTH),
                StringUtils.left(errorMsg, MAX_ERROR_MSG_LENGTH)
        );
    }

    private String defaultWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "local";
        }
    }
}
