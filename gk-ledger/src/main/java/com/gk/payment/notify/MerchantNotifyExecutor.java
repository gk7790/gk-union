package com.gk.payment.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.merchant.dao.MerchantAppDao;
import com.gk.merchant.entity.MerchantAppEntity;
import com.gk.payment.entity.MerchantNotifyRecordEntity;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
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
 * 商户异步通知发送/重试引擎。
 * <p>
 * 职责: 抢占到期任务 → 组装带签名的请求 → HTTP POST 商户 notify_url → 记录每次尝试 →
 * 按指数退避重试, 超过最大次数进入死信(DEAD)。
 * HTTP 调用在事务外执行, 仅落库阶段使用事务, 避免长事务占用连接。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantNotifyExecutor {
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DEAD = "DEAD";

    private final MerchantNotifyProperties properties;
    private final MerchantNotifyRepository repository;
    private final MerchantNotifySigner signer;
    private final MerchantAppDao merchantAppDao;
    private final ObjectMapper objectMapper;

    private String workerId;

    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(properties.getWorkerId())) {
            this.workerId = properties.getWorkerId();
            return;
        }
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "node";
        }
        this.workerId = host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 扫描并处理一批到期任务。
     *
     * @return 实际处理(抢占成功并尝试发送)的任务数
     */
    public int dispatchBatch() {
        Instant now = Instant.now();
        List<MerchantNotifyTaskEntity> candidates = repository.findClaimable(now, properties.getBatchSize());
        int handled = 0;
        for (MerchantNotifyTaskEntity task : candidates) {
            Instant lockUntil = Instant.now().plusSeconds(properties.getLockSeconds());
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
     * 后台手动重发: 强制抢占并立即同步发送一次, 返回本次是否成功。
     * 即使任务已 DEAD 也可重发(会自动再放开重试次数)。
     */
    public boolean resend(Long taskId) {
        Instant lockUntil = Instant.now().plusSeconds(properties.getLockSeconds());
        MerchantNotifyTaskEntity task = repository.forceClaim(taskId, workerId, Instant.now(), lockUntil);
        if (task == null) {
            return false;
        }
        return attempt(task, true);
    }

    /**
     * 执行一次通知尝试并落库。
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

        String apiSecret = loadApiSecret(task);
        HttpOutcome outcome;
        if (StringUtils.isBlank(apiSecret)) {
            outcome = HttpOutcome.transportError("merchant app api secret missing, merchantAppId=" + task.getMerchantAppId());
        } else {
            MerchantNotifySignature signature = signer.sign(task, apiSecret, payloadJson);
            record.setRequestSignature(signature.signature());
            record.setRequestHeadersJson(toJson(signature.headers()));
            task.setSignature(signature.signature());
            outcome = doPost(task, signature, payloadJson);
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
            task.setStatus(STATUS_SUCCESS);
            task.setSuccessAt(now);
            task.setDeadAt(null);
            return;
        }

        boolean exhausted = attemptNo >= maxRetry;
        if (exhausted && !manual) {
            task.setStatus(STATUS_DEAD);
            task.setDeadAt(now);
            return;
        }
        // 人工重发耗尽次数时, 放开一次, 重新挂回重试队列
        if (exhausted) {
            task.setMaxRetryCount(attemptNo + 1);
        }
        task.setStatus(STATUS_FAILED);
        task.setNextRetryAt(now.plusSeconds(properties.backoffSeconds(attemptNo)));
    }

    private HttpOutcome doPost(MerchantNotifyTaskEntity task, MerchantNotifySignature signature, String payloadJson) {
        int readTimeout = task.getTimeoutMs() == null ? properties.getDefaultTimeoutMs() : task.getTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(readTimeout);
        RestClient client = RestClient.builder().requestFactory(factory).build();
        try {
            return client.post()
                    .uri(URI.create(task.getNotifyUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> signature.headers().forEach(h::set))
                    .body(payloadJson)
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
        if (!properties.isRequireSuccessBody()) {
            return true;
        }
        String body = outcome.body();
        if (StringUtils.isBlank(body)) {
            return false;
        }
        String normalized = body.trim().toLowerCase(Locale.ROOT);
        List<String> tokens = properties.getSuccessBodyTokens();
        if (tokens == null || tokens.isEmpty()) {
            return true;
        }
        return tokens.stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(token -> normalized.contains(token.toLowerCase(Locale.ROOT)));
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

    private String loadApiSecret(MerchantNotifyTaskEntity task) {
        if (task.getMerchantAppId() == null) {
            return null;
        }
        MerchantAppEntity app = merchantAppDao.selectById(task.getMerchantAppId());
        // TODO: 若后续 api_secret 改为加密存储, 这里需先解密
        return app == null ? null : app.getApiSecret();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value) {
        return StringUtils.abbreviate(value, properties.getMaxStoreBodyLength());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 单次 HTTP 调用结果。status 为空表示传输层异常(连接超时/DNS等)。
     */
    private record HttpOutcome(Integer status, String body, String error) {
        static HttpOutcome transportError(String error) {
            return new HttpOutcome(null, null, error);
        }
    }
}
