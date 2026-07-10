package com.gk.telegram.alert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.gk.common.model.Result;
import com.gk.telegram.bot.TgBotApiClient;
import com.gk.telegram.dao.TgMessageTaskDao;
import com.gk.telegram.entity.TgBotEntity;
import com.gk.telegram.entity.TgMessageTaskEntity;
import com.gk.telegram.service.TgBotService;
import com.gk.telegram.support.TgConstants;
import com.gk.telegram.support.TgTokenCipher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Telegram 出站消息任务发送执行器。
 * <p>
 * 扫描 tg_message_task 中到期的 INIT/FAILED 任务，抢占锁后调用 Telegram Bot API 发送。
 * 发送失败会按退避时间重新调度，超过最大次数后进入 DEAD。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgMessageTaskExecutor {
    /** 单批最多扫描任务数，避免一次定时触发占用过久。 */
    private static final int BATCH_SIZE = 100;
    /** 单次定时触发最多连续排空批次数。 */
    private static final int MAX_DRAIN_LOOPS = 20;
    /** 抢占锁有效期；超时后其他节点可以重新抢占。 */
    private static final int LOCK_SECONDS = 120;
    /** 失败重试退避时间，按 retry_count 递增取值。 */
    private static final long[] BACKOFF_SECONDS = {15, 30, 60, 120, 300, 600, 1800, 3600};

    private final TgMessageTaskDao tgMessageTaskDao;
    private final TgBotService tgBotService;
    private final TgTokenCipher tokenCipher;
    private final TgBotApiClient botApiClient;

    /** 当前节点执行器标识，写入 locked_by 方便排查任务归属。 */
    private String workerId;

    /**
     * 初始化当前节点 workerId。
     */
    @PostConstruct
    public void init() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "node";
        }
        this.workerId = host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 排空当前到期 Telegram 消息任务。
     * <p>不传参数时兼容旧行为，处理所有 biz_type。</p>
     */
    public int drain() {
        return drain(null);
    }

    /**
     * 按定时任务参数排空当前到期 Telegram 消息任务。
     * <p>
     * 支持参数：SYSTEM_ALERT、BUSINESS_NOTIFY、ALERT、NOTIFY，
     * 也支持 JSON：{"bizType":"SYSTEM_ALERT"}。
     */
    public int drain(String params) {
        ensureWorkerId();
        String bizType = normalizeBizType(params);
        int total = 0;
        for (int loop = 0; loop < MAX_DRAIN_LOOPS; loop++) {
            int handled = dispatchBatch(bizType);
            total += handled;
            // 本批未打满，说明暂时没有更多到期任务，结束本轮排空。
            if (handled < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    /**
     * 扫描并处理一批到期任务。
     */
    public int dispatchBatch() {
        return dispatchBatch(null);
    }

    /**
     * 扫描并处理一批指定 biz_type 的到期任务。
     */
    public int dispatchBatch(String bizType) {
        ensureWorkerId();
        Instant now = Instant.now();
        List<TgMessageTaskEntity> candidates = findClaimable(now, BATCH_SIZE, bizType);
        int handled = 0;
        for (TgMessageTaskEntity task : candidates) {
            // 先抢占任务，避免集群中多个节点同时发送同一条 Telegram 消息。
            if (!claim(task, now, now.plusSeconds(LOCK_SECONDS))) {
                continue;
            }
            handled++;
            try {
                attempt(task);
            } catch (Exception e) {
                log.error("Telegram message task attempt error, taskNo={}", task.getTaskNo(), e);
                applyResult(task, false, e.getMessage(), null);
            }
        }
        return handled;
    }

    /**
     * 立即发送指定消息任务一次。
     * <p>
     * 适合后台人工触发的通知：任务仍然先落库，便于审计和失败重试；随后立即抢占并调用 Telegram。
     */
    public boolean sendNow(Long taskId) {
        ensureWorkerId();
        if (taskId == null) {
            return false;
        }
        TgMessageTaskEntity task = tgMessageTaskDao.selectById(taskId);
        if (task == null) {
            return false;
        }
        Instant now = Instant.now();
        if (!claim(task, now, now.plusSeconds(LOCK_SECONDS))) {
            return false;
        }
        try {
            return attempt(task);
        } catch (Exception e) {
            log.error("Telegram message task send now error, taskNo={}", task.getTaskNo(), e);
            applyResult(task, false, e.getMessage(), null);
            return false;
        }
    }

    /**
     * 尝试发送单条 Telegram 消息任务。
     *
     * @return true 表示 Telegram API 返回成功；false 表示已记录失败结果
     */
    boolean attempt(TgMessageTaskEntity task) {
        TgBotEntity bot = tgBotService.selectById(task.getBotId());
        if (bot == null || StringUtils.isBlank(bot.getTokenCipher())) {
            applyResult(task, false, "Telegram bot not found or token missing", null);
            return false;
        }
        JSONObject payload = parsePayload(task.getPayloadJson());
        String text = task.getContent();
        if (StringUtils.isBlank(text)) {
            applyResult(task, false, "Telegram message text is blank", null);
            return false;
        }
        String token = tokenCipher.decrypt(bot.getTokenCipher());
        Result<JSONObject> result = botApiClient.sendMessage(token, task.getChatId(), text, task.getParseMode(), payload);
        if (result.isSuccess()) {
            Long messageId = result.getData() == null ? null : result.getData().getLong("message_id");
            applyResult(task, true, null, messageId);
            return true;
        }
        applyResult(task, false, result.getMsg(), null);
        return false;
    }

    /**
     * 查找可被当前节点抢占的到期任务。
     */
    private List<TgMessageTaskEntity> findClaimable(Instant now, int batchSize, String bizType) {
        QueryWrapper<TgMessageTaskEntity> wrapper = new QueryWrapper<TgMessageTaskEntity>()
                .in("status", "INIT", "FAILED")
                .le("next_retry_at", now)
                // 只处理仍有重试次数的任务，超过次数的任务会保持 DEAD/不再进入这里。
                .apply("retry_count < max_retry_count")
                // 锁为空或锁已过期才允许被当前节点抢占。
                .and(w -> w.isNull("lock_until").or().le("lock_until", now))
                .orderByAsc("next_retry_at")
                .orderByAsc("id")
                .last("LIMIT " + Math.max(1, batchSize));
        if (StringUtils.isNotBlank(bizType)) {
            wrapper.eq("biz_type", bizType);
        }
        return tgMessageTaskDao.selectList(wrapper);
    }

    /**
     * 抢占任务并写入 PROCESSING 状态。
     * <p>update 条件中带上原状态和锁时间，保证并发抢占时只有一个节点成功。</p>
     */
    private boolean claim(TgMessageTaskEntity task, Instant now, Instant lockUntil) {
        return tgMessageTaskDao.update(null, new UpdateWrapper<TgMessageTaskEntity>()
                .eq("id", task.getId())
                .eq("status", task.getStatus())
                .and(w -> w.isNull("lock_until").or().le("lock_until", now))
                .set("status", "PROCESSING")
                .set("locked_by", workerId)
                .set("lock_until", lockUntil)
                .set("last_attempt_at", now)) == 1;
    }

    /**
     * 应用发送结果。
     * <p>成功则标记 SUCCESS；失败则增加 retry_count，并按退避时间计算 next_retry_at。</p>
     */
    private void applyResult(TgMessageTaskEntity task, boolean success, String errorMsg, Long tgMessageId) {
        Instant now = Instant.now();
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? 8 : task.getMaxRetryCount();
        UpdateWrapper<TgMessageTaskEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId())
                .set("last_attempt_at", now)
                .set("locked_by", null)
                .set("lock_until", null);
        if (success) {
            wrapper.set("status", "SUCCESS")
                    .set("tg_message_id", tgMessageId)
                    .set("success_at", now)
                    .set("last_error_msg", null);
        } else {
            int nextRetryCount = retryCount + 1;
            boolean dead = nextRetryCount >= maxRetryCount;
            wrapper.set("status", dead ? "DEAD" : "FAILED")
                    .set("retry_count", nextRetryCount)
                    .set("next_retry_at", now.plusSeconds(backoffSeconds(nextRetryCount)))
                    .set("last_error_msg", StringUtils.abbreviate(errorMsg, 1024))
                    .set("dead_at", dead ? now : null);
        }
        tgMessageTaskDao.update(null, wrapper);
    }

    /**
     * 根据当前失败次数计算下一次重试等待秒数。
     */
    private long backoffSeconds(int retryCount) {
        int index = Math.max(0, retryCount - 1);
        if (index >= BACKOFF_SECONDS.length) {
            return BACKOFF_SECONDS[BACKOFF_SECONDS.length - 1];
        }
        return BACKOFF_SECONDS[index];
    }

    /**
     * 将定时任务 params 解析为 tg_message_task.biz_type。
     */
    private String normalizeBizType(String params) {
        if (StringUtils.isBlank(params)) {
            return null;
        }
        String value = params.trim();
        if (value.startsWith("{")) {
            try {
                JSONObject json = JSON.parseObject(value);
                value = StringUtils.defaultIfBlank(json.getString("bizType"), json.getString("type"));
            } catch (Exception ignored) {
                return null;
            }
        }
        if ("ALERT".equalsIgnoreCase(value) || "SYSTEM".equalsIgnoreCase(value)
                || TgConstants.MessageBizType.SYSTEM_ALERT.equalsIgnoreCase(value)) {
            return TgConstants.MessageBizType.SYSTEM_ALERT;
        }
        if ("NOTIFY".equalsIgnoreCase(value) || "BUSINESS".equalsIgnoreCase(value)
                || TgConstants.MessageBizType.BUSINESS_NOTIFY.equalsIgnoreCase(value)) {
            return TgConstants.MessageBizType.BUSINESS_NOTIFY;
        }
        return null;
    }

    /**
     * 解析任务 payload_json。
     */
    private JSONObject parsePayload(String payloadJson) {
        try {
            return JSON.parseObject(payloadJson);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 测试或特殊初始化顺序下兜底生成 workerId。
     */
    private void ensureWorkerId() {
        if (workerId == null) {
            init();
        }
    }
}
