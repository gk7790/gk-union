package com.gk.telegram.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.transaction.SavepointExecutor;
import com.gk.telegram.dao.TgUpdateLogDao;
import com.gk.telegram.entity.TgUpdateLogEntity;
import com.gk.telegram.service.TgUpdateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Telegram 入站 update 日志服务实现。
 * <p>通过 bot_id + update_id 唯一约束实现 webhook 幂等处理，并记录处理结果。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TgUpdateLogServiceImpl implements TgUpdateLogService {
    private final TgUpdateLogDao tgUpdateLogDao;

    /**
     * 尝试登记 Telegram update，依赖 bot_id + update_id 唯一键实现幂等。
     */
    @Override
    public boolean tryBegin(TgUpdateLogEntity log) {
        try {
            if (log.getCreatedAt() == null) {
                log.setCreatedAt(Instant.now());
            }
            if (log.getHandleStatus() == null) {
                log.setHandleStatus(0);
            }
            SavepointExecutor.run(() -> tgUpdateLogDao.insert(log));
            return true;
        } catch (DuplicateKeyException e) {
            // bot_id + update_id 已存在, 说明重复投递, 幂等跳过
            return false;
        }
    }

    /**
     * 标记入站 update 的最终处理状态。
     */
    @Override
    public void markResult(Long id, int handleStatus, String errorMsg) {
        if (id == null) {
            return;
        }
        tgUpdateLogDao.update(null, new UpdateWrapper<TgUpdateLogEntity>()
                .eq("id", id)
                .set("handle_status", handleStatus)
                .set("error_msg", errorMsg));
    }
}
