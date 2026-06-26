package com.gk.psp.callback.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
import com.gk.psp.enums.PspCallbackProcessStatusEnum;
import com.gk.psp.enums.PspCallbackVerifyStatusEnum;
import com.gk.psp.dao.PspProviderDao;
import com.gk.psp.entity.PspCallbackLogEntity;
import com.gk.psp.entity.PspProviderEntity;
import com.gk.psp.service.PspCallbackLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * PSP 回调日志记录器 * <p>
 * 负责记录 PSP 回调的请求头、请求体、验签状态、处理状态和错误信息 * 日志{@link #finish} 时异步落库，避免阻塞回调主链路；失败只记 warn，不影响订单处理 */
@Component
@Slf4j
public class PspCallbackLogRecorder {
    private final PspCallbackLogService pspCallbackLogService;
    private final PspProviderDao pspProviderDao;
    private final ObjectMapper objectMapper;
    private final Executor pspCallbackLogExecutor;

    public PspCallbackLogRecorder(
            PspCallbackLogService pspCallbackLogService,
            PspProviderDao pspProviderDao,
            ObjectMapper objectMapper,
            @Qualifier("pspCallbackLogExecutor") Executor pspCallbackLogExecutor
    ) {
        this.pspCallbackLogService = pspCallbackLogService;
        this.pspProviderDao = pspProviderDao;
        this.objectMapper = objectMapper;
        this.pspCallbackLogExecutor = pspCallbackLogExecutor;
    }

    /**
     * 构建已成功定位订单的 PSP 回调日志上下文     * <p>
     * 仅生成内存实体与 traceId，实际落库在 {@link #finish} 异步执行     *
     * @param request 标准回调请求
     * @param result  PSP 标准回调结果
     * @param order   平台订单快照
     * @return 待落库的回调日志实体
     */
    public PspCallbackLogEntity received(PspCallbackRequest request, PspCallbackResult result, PspCallbackOrder order) {
        PspCallbackLogEntity entity = new PspCallbackLogEntity();
        entity.setTenantId(PspCallbackUtils.defaultLong(order.tenantId()));
        entity.setMerchantId(order.merchantId());
        entity.setPspId(PspCallbackUtils.defaultLong(order.pspId()));
        entity.setPspCode(StringUtils.defaultIfBlank(order.pspCode(), request.getPspCode()));
        entity.setBizType(request.getBizType());
        entity.setBizId(order.id());
        entity.setBizNo(order.orderNo());
        entity.setPspOrderNo(result.getPspOrderNo());
        entity.setCallbackType(StringUtils.defaultIfBlank(result.getCallbackType(), request.getBizType()));
        entity.setCallbackId(result.getCallbackId());
        entity.setBodyHash(PspCallbackUtils.sha256Hex(StringUtils.defaultString(request.getRawBody())));
        entity.setCallbackKey(callbackKey(entity));
        entity.setHeadersJson(toJson(request.getHeaders()));
        entity.setBodyJson(toJson(request.getParams()));
        entity.setRawBody(request.getRawBody());
        entity.setSignature(result.getSignature());
        entity.setVerifyStatus(PspCallbackVerifyStatusEnum.INIT.code());
        entity.setProcessStatus(PspCallbackProcessStatusEnum.INIT.code());
        entity.setReceivedAt(Instant.now());
        entity.setTraceId(newTraceId());
        return entity;
    }

    /**
     * 构建尚未定位到订单的 PSP 回调日志上下文     * <p>
     * 即使还没有订单上下文，也会尽量保留原始请求，方便后续人工排查     */
    public PspCallbackLogEntity failed(String pspCode, String bizType, PspCallbackRequest request, Exception ex) {
        PspProviderEntity provider = pspProviderDao.selectOne(new QueryWrapper<PspProviderEntity>()
                .eq("psp_code", pspCode)
                .last("limit 1"));
        PspCallbackLogEntity entity = new PspCallbackLogEntity();
        entity.setTenantId(0L);
        entity.setPspId(provider == null ? 0L : provider.getId());
        entity.setPspCode(pspCode);
        entity.setBizType(bizType);
        entity.setCallbackType(bizType);
        entity.setBodyHash(PspCallbackUtils.sha256Hex(request == null ? null : request.getRawBody()));
        entity.setCallbackKey(bizType + ":" + entity.getBodyHash());
        entity.setHeadersJson(request == null ? null : toJson(request.getHeaders()));
        entity.setBodyJson(request == null ? null : toJson(request.getParams()));
        entity.setRawBody(request == null ? null : request.getRawBody());
        entity.setReceivedAt(Instant.now());
        entity.setTraceId(newTraceId());
        if (ex != null) {
            entity.setErrorMsg(StringUtils.left(ex.getMessage(), 1024));
        }
        return entity;
    }

    /**
     * 完成回调日志并异步落库     *
     * @param entity        回调日志实体
     * @param verifyStatus  验签状     * @param processStatus 业务处理状     * @param errorMsg      错误信息，成功时可为     */
    public void finish(PspCallbackLogEntity entity, String verifyStatus, String processStatus, String errorMsg) {
        if (entity == null) {
            return;
        }
        entity.setVerifyStatus(verifyStatus);
        entity.setProcessStatus(processStatus);
        if (StringUtils.isNotBlank(errorMsg)) {
            entity.setErrorMsg(StringUtils.left(errorMsg, 1024));
        }
        entity.setProcessedAt(Instant.now());
        submit(entity);
    }

    /**
     * 异步提交回调日志     */
    private void submit(PspCallbackLogEntity entity) {
        try {
            pspCallbackLogExecutor.execute(() -> pspCallbackLogService.record(entity));
        } catch (Exception ex) {
            log.warn("Submit PSP callback log failed: {}", ex.getMessage());
        }
    }

    /**
     * 生成回调幂等键     * <p>
     * PSP 提供 callbackId 时优先使用；否则使用回调类型bodyHash 兜底     */
    private String callbackKey(PspCallbackLogEntity entity) {
        if (StringUtils.isNotBlank(entity.getCallbackId())) {
            return entity.getCallbackId();
        }
        return entity.getCallbackType() + ":" + entity.getBodyHash();
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 对象JSON     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
