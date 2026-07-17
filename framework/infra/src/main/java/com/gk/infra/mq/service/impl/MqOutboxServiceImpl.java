package com.gk.infra.mq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.transaction.SavepointExecutor;
import com.gk.infra.mq.dao.MqOutboxDao;
import com.gk.infra.mq.entity.MqOutboxEntity;
import com.gk.infra.mq.enums.MqOutboxConsumeStatusEnum;
import com.gk.infra.mq.enums.MqOutboxPublishStatusEnum;
import com.gk.infra.mq.service.MqOutboxService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MqOutboxServiceImpl implements MqOutboxService {
    private static final int DEFAULT_LOCK_SECONDS = 120;
    private static final int MAX_ERROR_CODE_LENGTH = 64;
    private static final int MAX_ERROR_MSG_LENGTH = 1024;

    private final MqOutboxDao mqOutboxDao;
    private final String workerId = defaultWorkerId() + "-" + UUID.randomUUID();

    @Override
    public MqOutboxEntity createIfAbsent(MqOutboxEntity event) {
        if (event == null) {
            throw new IllegalArgumentException("mq outbox event is required");
        }
        applyDefaults(event);
        try {
            SavepointExecutor.run(() -> mqOutboxDao.insert(event));
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

    private void applyDefaults(MqOutboxEntity event) {
        if (StringUtils.isBlank(event.getPublishStatus())) {
            event.setPublishStatus(MqOutboxPublishStatusEnum.INIT.code());
        }
        if (StringUtils.isBlank(event.getConsumeStatus())) {
            event.setConsumeStatus(MqOutboxConsumeStatusEnum.INIT.code());
        }
        if (event.getRetryCount() == null) {
            event.setRetryCount(0);
        }
        if (event.getMaxRetryCount() == null) {
            event.setMaxRetryCount(16);
        }
        if (event.getNextRetryAt() == null) {
            event.setNextRetryAt(Instant.now());
        }
    }

    @Override
    public List<MqOutboxEntity> lockDueEvents(String eventType, int limit, String workerId) {
        Instant now = Instant.now();
        String lockOwner = StringUtils.defaultIfBlank(workerId, this.workerId);
        List<MqOutboxEntity> candidates = mqOutboxDao.selectDueForConsume(
                eventType,
                now,
                limit,
                MqOutboxConsumeStatusEnum.INIT.code(),
                MqOutboxConsumeStatusEnum.FAILED.code(),
                MqOutboxConsumeStatusEnum.LOCKED.code()
        );
        List<MqOutboxEntity> locked = new ArrayList<>();
        for (MqOutboxEntity candidate : candidates) {
            int updated = mqOutboxDao.lockForConsume(
                    candidate.getId(),
                    lockOwner,
                    now,
                    now.plusSeconds(DEFAULT_LOCK_SECONDS),
                    MqOutboxConsumeStatusEnum.INIT.code(),
                    MqOutboxConsumeStatusEnum.FAILED.code(),
                    MqOutboxConsumeStatusEnum.LOCKED.code()
            );
            if (updated == 1) {
                candidate.setConsumeStatus(MqOutboxConsumeStatusEnum.LOCKED.code());
                candidate.setLockedBy(lockOwner);
                locked.add(candidate);
            }
        }
        return locked;
    }

    @Override
    public void markDone(Long id, String workerId) {
        int updated = mqOutboxDao.markConsumeDone(
                id,
                workerId,
                Instant.now(),
                MqOutboxConsumeStatusEnum.DONE.code(),
                MqOutboxConsumeStatusEnum.LOCKED.code()
        );
        requireLockOwner(updated, id, workerId, "done");
    }

    @Override
    public void markRetry(Long id, String workerId, String errorCode, String errorMsg) {
        Instant nextRetryAt = Instant.now().plusSeconds(30);
        int updated = mqOutboxDao.markConsumeFailed(
                id,
                workerId,
                nextRetryAt,
                StringUtils.left(errorCode, MAX_ERROR_CODE_LENGTH),
                StringUtils.left(errorMsg, MAX_ERROR_MSG_LENGTH),
                MqOutboxConsumeStatusEnum.FAILED.code(),
                MqOutboxConsumeStatusEnum.LOCKED.code()
        );
        if (updated != 1) {
            markDead(id, workerId, errorCode, errorMsg);
        }
    }

    @Override
    public void markDead(Long id, String workerId, String errorCode, String errorMsg) {
        int updated = mqOutboxDao.markConsumeDead(
                id,
                workerId,
                Instant.now(),
                StringUtils.left(errorCode, MAX_ERROR_CODE_LENGTH),
                StringUtils.left(errorMsg, MAX_ERROR_MSG_LENGTH),
                MqOutboxConsumeStatusEnum.DEAD.code(),
                MqOutboxConsumeStatusEnum.LOCKED.code()
        );
        requireLockOwner(updated, id, workerId, "dead");
    }

    private void requireLockOwner(int updated, Long id, String workerId, String operation) {
        if (updated != 1) {
            throw new IllegalStateException("MQ outbox lock lost while marking " + operation
                    + ", id=" + id + ", workerId=" + workerId);
        }
    }

    private String defaultWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "local";
        }
    }
}
