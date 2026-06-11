package com.gk.payment.notify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.payment.dao.MerchantNotifyRecordDao;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.entity.MerchantNotifyRecordEntity;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 商户通知任务的抢占与落库。
 * <p>
 * 通过"条件更新"实现行级抢占, 多实例/多线程并发安全:
 * 只有把 status 从可处理态原子地改为 PROCESSING 并写入锁的那个节点才算抢到任务。
 */
@Repository
@RequiredArgsConstructor
public class MerchantNotifyRepository {
    /** 可被扫描处理的状态 */
    private static final String STATUS_INIT = "INIT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final MerchantNotifyRecordDao merchantNotifyRecordDao;

    /**
     * 查找到期且未被锁定的候选任务。
     */
    public List<MerchantNotifyTaskEntity> findClaimable(Instant now, int batchSize) {
        QueryWrapper<MerchantNotifyTaskEntity> wrapper = new QueryWrapper<>();
        wrapper.in("status", STATUS_INIT, STATUS_FAILED)
                .le("next_retry_at", now)
                .apply("retry_count < max_retry_count")
                .and(w -> w.isNull("lock_until").or().le("lock_until", now))
                .orderByAsc("next_retry_at")
                .orderByAsc("id")
                .last("LIMIT " + Math.max(1, batchSize));
        return merchantNotifyTaskDao.selectList(wrapper);
    }

    /**
     * 原子抢占一个候选任务。
     *
     * @return true 表示抢占成功(本节点拥有该任务)
     */
    public boolean claim(MerchantNotifyTaskEntity task, String workerId, Instant now, Instant lockUntil) {
        UpdateWrapper<MerchantNotifyTaskEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId())
                .eq("status", task.getStatus())
                .and(w -> w.isNull("lock_until").or().le("lock_until", now))
                .set("status", STATUS_PROCESSING)
                .set("locked_by", workerId)
                .set("locked_at", now)
                .set("lock_until", lockUntil);
        return merchantNotifyTaskDao.update(null, wrapper) == 1;
    }

    /**
     * 手动重发: 强制抢占(忽略 next_retry_at), 但仍尊重锁避免与扫描线程并发。
     * SUCCESS 任务不再处理。
     *
     * @return 抢占成功返回最新任务, 否则返回 null
     */
    public MerchantNotifyTaskEntity forceClaim(Long id, String workerId, Instant now, Instant lockUntil) {
        MerchantNotifyTaskEntity task = merchantNotifyTaskDao.selectById(id);
        if (task == null || STATUS_SUCCESS.equals(task.getStatus())) {
            return null;
        }
        UpdateWrapper<MerchantNotifyTaskEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .ne("status", STATUS_SUCCESS)
                .and(w -> w.isNull("lock_until").or().le("lock_until", now))
                .set("status", STATUS_PROCESSING)
                .set("locked_by", workerId)
                .set("locked_at", now)
                .set("lock_until", lockUntil);
        if (merchantNotifyTaskDao.update(null, wrapper) != 1) {
            return null;
        }
        return merchantNotifyTaskDao.selectById(id);
    }

    public MerchantNotifyTaskEntity getById(Long id) {
        return merchantNotifyTaskDao.selectById(id);
    }

    /**
     * 一次通知尝试的结果落库: 写一条通知记录 + 更新任务终态/重试态, 同一事务保证一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistAttempt(MerchantNotifyRecordEntity record, MerchantNotifyTaskEntity task) {
        try {
            merchantNotifyRecordDao.insert(record);
        } catch (DuplicateKeyException ignored) {
            // 同一 attempt_no 已写入(并发/重复触发), 记录幂等跳过, 仍更新任务状态。
        }
        UpdateWrapper<MerchantNotifyTaskEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId())
                .set("status", task.getStatus())
                .set("retry_count", task.getRetryCount())
                .set("max_retry_count", task.getMaxRetryCount())
                .set("next_retry_at", task.getNextRetryAt())
                .set("signature", task.getSignature())
                .set("last_http_status", task.getLastHttpStatus())
                .set("last_response_body", task.getLastResponseBody())
                .set("last_error_msg", task.getLastErrorMsg())
                .set("last_attempt_at", task.getLastAttemptAt())
                .set("success_at", task.getSuccessAt())
                .set("dead_at", task.getDeadAt())
                // 释放锁
                .set("locked_by", null)
                .set("locked_at", null)
                .set("lock_until", null);
        merchantNotifyTaskDao.update(null, wrapper);
    }
}
