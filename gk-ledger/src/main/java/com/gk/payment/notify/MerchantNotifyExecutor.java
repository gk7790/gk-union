package com.gk.payment.notify;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.model.Result;
import com.gk.common.validator.AssertUtils;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.MerchantNotifyRecordEntity;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.enums.SignTypeEnum;
import com.gk.payment.enums.MerchantNotifyTaskStatusEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 商户异步通知发�?重试引擎�?
 * <p>
 * 职责: 抢占到期任务 �?对报文签�?sign 写入 body) �?HTTP POST 商户 notify_url �?记录每次尝试 �?
 * 按指数退避重�? 超过最大次数进入死�?DEAD)�?
 * 触发方式�?{@link MerchantNotifyTask}(�?gk-scheduler �?Quartz 定时任务驱动)�?
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantNotifyExecutor {

    /** 单批抢占任务�?*/
    private static final int BATCH_SIZE = 100;
    /** 一次触发最多连续处理的批次�?防止单次触发占用过久) */
    private static final int MAX_DRAIN_LOOPS = 20;
    /** 任务锁定时长(�?: 抢占后多久未完成视为可被其他节点重新抢占 */
    private static final int LOCK_SECONDS = 120;
    /** HTTP 连接超时(毫秒) */
    private static final int CONNECT_TIMEOUT_MS = 3000;
    /** 任务未配�?timeoutMs 时的默认读取超时(毫秒) */
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;
    /** 落库的响应体/错误信息最大长�?*/
    private static final int MAX_STORE_LEN = 2000;
    /** 手动重发失败时返回给前端的错误信息最大长�?*/
    private static final int MAX_FAIL_MSG_LEN = 300;
    /** 视为成功的响应体标识(忽略大小�? 命中其一即成�? */
    private static final List<String> SUCCESS_TOKENS = List.of("success", "ok");
    /** 重试退避秒�?按已失败次数取下�? 超出取最后一�? */
    private static final long[] BACKOFF_SECONDS = {15, 30, 60, 120, 300, 600, 1800, 3600, 7200, 21600};

    private final MerchantNotifyRepository repository;
    private final MerchantNotifySigner signer;
    private final MerchantAppDao merchantAppDao;
    private final MerchantOrderNotifyStatusService merchantOrderNotifyStatusService;
    private final MerchantNotifyTaskDao merchantNotifyTaskDao;
    private final PayOrderDao payOrderDao;
    private final PayoutOrderDao payoutOrderDao;

    private String workerId;

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
     * 排空式处�? 连续处理多批直到没有到期任务或达到上限。供定时任务一次触发调用�?
     *
     * @return 本次累计处理的任务数
     */
    public int drain() {
        int total = 0;
        for (int loop = 0; loop < MAX_DRAIN_LOOPS; loop++) {
            int handled = dispatchBatch();
            total += handled;
            if (handled < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    /**
     * 扫描并处理一批到期任务�?
     *
     * @return 实际处理(抢占成功并尝试发�?的任务数
     */
    public int dispatchBatch() {
        Instant now = Instant.now();
        List<MerchantNotifyTaskEntity> candidates = repository.findClaimable(now, BATCH_SIZE);
        int handled = 0;
        for (MerchantNotifyTaskEntity task : candidates) {
            Instant lockUntil = Instant.now().plusSeconds(LOCK_SECONDS);
            if (!repository.claim(task, workerId, Instant.now(), lockUntil)) {
                // 已被其他节点抢占, 跳过
                continue;
            }
            handled++;
            try {
                attempt(task, false);
            } catch (Exception e) {
                log.error("Merchant notify attempt error, taskNo={}", task.getTaskNo(), e);
            }
        }
        return handled;
    }

    /**
     * 后台手动重发: 强制抢占并立即同步发送一次�?
     * 即使任务�?DEAD 也可重发(会自动再放开重试次数)�?
     */
    public Result<Void> resend(Long taskId) {
        MerchantNotifyTaskEntity existing = repository.getById(taskId);
        if (existing == null) {
            return Result.fail("通知任务不存�?);
        }
        if (MerchantNotifyTaskStatusEnum.SUCCESS.matches(existing.getStatus())) {
            return Result.fail("通知已成�? 无需重复发�?);
        }
        Instant lockUntil = Instant.now().plusSeconds(LOCK_SECONDS);
        MerchantNotifyTaskEntity task = repository.forceClaim(taskId, workerId, Instant.now(), lockUntil);
        if (task == null) {
            return Result.fail("通知任务正在处理�? 请稍后再�?);
        }
        try {
            if (attempt(task, true)) {
                return Result.success(null, "通知成功");
            }
            String message = StringUtils.defaultIfBlank(
                    StringUtils.abbreviate(task.getLastErrorMsg(), MAX_FAIL_MSG_LEN),
                    "商户未确认通知");
            return Result.fail(message);
        } catch (Exception e) {
            log.error("Merchant notify resend error, taskId={}", taskId, e);
            return Result.fail("通知发送异�? {}", e.getMessage());
        }
    }

    public Result<Void> resendPayOrder(Long orderId) {
        AssertUtils.isNull(orderId, "id");
        return resendByBizOrder(BizTypeEnum.PAY_ORDER.code(), orderId);
    }

    public Result<Void> resendPayoutOrder(Long orderId) {
        AssertUtils.isNull(orderId, "id");
        return resendByBizOrder(BizTypeEnum.PAYOUT_ORDER.code(), orderId);
    }

    private Result<Void> resendByBizOrder(String bizType, Long orderId) {
        MerchantNotifyTaskEntity task = merchantNotifyTaskDao.selectOne(new QueryWrapper<MerchantNotifyTaskEntity>()
                .eq("biz_type", bizType)
                .eq("biz_id", orderId)
                .orderByDesc("created_at")
                .last("limit 1"));
        if (task == null) {
            return Result.fail("该订单暂无商户通知任务");
        }
        return resend(task.getId());
    }

    /**
     * 执行一次通知尝试并落库�?
     *
     * @param manual 是否人工触发(人工触发失败不直接进死信, 而是重新挂回重试队列)
     * @return 本次是否成功
     */
    private boolean attempt(MerchantNotifyTaskEntity task, boolean manual) {
        int attemptNo = safeInt(task.getRetryCount()) + 1;
        int maxRetry = task.getMaxRetryCount() == null ? 16 : task.getMaxRetryCount();
        String payloadJson = task.getPayloadJson();
        Instant startedAt = Instant.now();

        MerchantNotifyRecordEntity record = new MerchantNotifyRecordEntity();
        record.setTenantId(task.getTenantId());
        record.setNotifyTaskId(task.getId());
        record.setTaskNo(task.getTaskNo());
        record.setAttemptNo(attemptNo);
        record.setNotifyUrl(task.getNotifyUrl());
        record.setHttpMethod("POST");
        record.setContentType(StringUtils.defaultIfBlank(task.getContentType(), "application/json"));
        record.setRequestBody(payloadJson);
        record.setStartedAt(startedAt);
        record.setTraceId(task.getTraceId());

        MerchantAppEntity app = loadApp(task);
        String apiSecret = app == null ? null : app.getApiSecret();
        HttpOutcome outcome;
        if (StringUtils.isBlank(apiSecret)) {
            outcome = HttpOutcome.transportError("merchant app api secret missing, merchantAppId=" + task.getMerchantAppId());
        } else {
            MerchantNotifySigned signed = signer.sign(payloadJson, apiSecret, resolveSignType(app, task));
            record.setRequestSignature(signed.sign());
            // 最终发送的报文(�?sign), 覆盖原始 payload
            record.setRequestBody(signed.body());
            task.setSignature(signed.sign());
            outcome = doPost(task, signed.body());
        }

        long costMs = System.currentTimeMillis() - startedAt.toEpochMilli();
        boolean success = isSuccess(outcome);

        record.setResponseStatus(outcome.status());
        record.setResponseBody(truncate(outcome.body()));
        record.setSuccess(success ? 1 : 0);
        record.setErrorMsg(success ? null : truncate(failReason(outcome)));
        record.setCostMs(costMs);
        record.setFinishedAt(Instant.now());

        applyResultToTask(task, attemptNo, maxRetry, manual, success, outcome);
        repository.persistAttempt(record, task);
        merchantOrderNotifyStatusService.syncFromTask(task);

        if (success) {
            log.info("Merchant notify success, taskNo={}, attempt={}, cost={}ms", task.getTaskNo(), attemptNo, costMs);
        } else {
            log.warn("Merchant notify failed, taskNo={}, attempt={}, status={}, err={}",
                    task.getTaskNo(), attemptNo, outcome.status(), failReason(outcome));
        }
        return success;
    }

    private void applyResultToTask(MerchantNotifyTaskEntity task, int attemptNo, int maxRetry,
                                   boolean manual, boolean success, HttpOutcome outcome) {
        Instant now = Instant.now();
        task.setRetryCount(attemptNo);
        task.setLastHttpStatus(outcome.status());
        task.setLastResponseBody(truncate(outcome.body()));
        task.setLastErrorMsg(success ? null : truncate(failReason(outcome)));
        task.setLastAttemptAt(now);

        if (success) {
            task.setStatus(MerchantNotifyTaskStatusEnum.SUCCESS.code());
            task.setSuccessAt(now);
            task.setDeadAt(null);
            return;
        }

        boolean exhausted = attemptNo >= maxRetry;
        if (exhausted && !manual) {
            task.setStatus(MerchantNotifyTaskStatusEnum.DEAD.code());
            task.setDeadAt(now);
            return;
        }
        // 人工重发耗尽次数�? 放开一�? 重新挂回重试队列
        if (exhausted) {
            task.setMaxRetryCount(attemptNo + 1);
        }
        task.setStatus(MerchantNotifyTaskStatusEnum.FAILED.code());
        task.setNextRetryAt(now.plusSeconds(backoffSeconds(attemptNo)));
    }

    private HttpOutcome doPost(MerchantNotifyTaskEntity task, String bodyJson) {
        int readTimeout = task.getTimeoutMs() == null ? DEFAULT_READ_TIMEOUT_MS : task.getTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(readTimeout);
        RestClient client = RestClient.builder().requestFactory(factory).build();
        try {
            return client.post()
                    .uri(URI.create(task.getNotifyUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyJson)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        String body = response.bodyTo(String.class);
                        return new HttpOutcome(status, body, null);
                    });
        } catch (Exception e) {
            return HttpOutcome.transportError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private boolean isSuccess(HttpOutcome outcome) {
        if (outcome.status() == null) {
            return false;
        }
        boolean httpOk = outcome.status() >= 200 && outcome.status() < 300;
        if (!httpOk) {
            return false;
        }
        String body = outcome.body();
        if (StringUtils.isBlank(body)) {
            return false;
        }
        String normalized = body.trim().toLowerCase(Locale.ROOT);
        return SUCCESS_TOKENS.stream().anyMatch(normalized::contains);
    }

    private String failReason(HttpOutcome outcome) {
        if (outcome.error() != null) {
            return outcome.error();
        }
        if (outcome.status() != null && (outcome.status() < 200 || outcome.status() >= 300)) {
            return "http status " + outcome.status();
        }
        return "response body not acknowledged";
    }

    private MerchantAppEntity loadApp(MerchantNotifyTaskEntity task) {
        if (task.getMerchantAppId() == null) {
            return null;
        }
        // TODO: 若后�?api_secret 改为加密存储, 这里需先解�?
        return merchantAppDao.selectById(task.getMerchantAppId());
    }

    private String resolveSignType(MerchantAppEntity app, MerchantNotifyTaskEntity task) {
        String signType = app == null ? null : app.getSignType();
        if (StringUtils.isBlank(signType)) {
            signType = task.getSignType();
        }
        return StringUtils.defaultIfBlank(signType, SignTypeEnum.MD5.code());
    }

    private long backoffSeconds(int failedTimes) {
        int index = Math.max(0, failedTimes - 1);
        if (index >= BACKOFF_SECONDS.length) {
            index = BACKOFF_SECONDS.length - 1;
        }
        return BACKOFF_SECONDS[index];
    }

    private String truncate(String value) {
        return StringUtils.abbreviate(value, MAX_STORE_LEN);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 单次 HTTP 调用结果。status 为空表示传输层异�?连接超时/DNS�?�?
     */
    private record HttpOutcome(Integer status, String body, String error) {
        static HttpOutcome transportError(String error) {
            return new HttpOutcome(null, null, error);
        }
    }
}
