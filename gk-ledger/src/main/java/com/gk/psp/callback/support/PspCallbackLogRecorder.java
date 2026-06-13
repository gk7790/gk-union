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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * PSP 回调日志记录器。
 * <p>
 * 负责记录 PSP 回调的请求头、请求体、验签状态、处理状态和错误信息。
 * 正常回调和异常回调都会尽量落库，便于后续排查、审计和人工补偿。
 */
@Component
@RequiredArgsConstructor
public class PspCallbackLogRecorder {
    private final PspCallbackLogService pspCallbackLogService;
    private final PspProviderDao pspProviderDao;
    private final ObjectMapper objectMapper;

    /**
     * 记录已成功定位订单的 PSP 回调。
     *
     * @param request 标准回调请求
     * @param result PSP 标准回调结果
     * @param order 平台订单快照
     * @return 已落库或尝试落库的回调日志实体
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
        // traceId 用于串联回调日志、订单状态变更和商户通知任务。
        entity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        insert(entity);
        return entity;
    }

    /**
     * 记录解析、验签或定位订单失败的 PSP 回调。
     * <p>
     * 即使还没有订单上下文，也会尽量保留原始请求，方便后续人工排查。
     */
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
        entity.setVerifyStatus(PspCallbackVerifyStatusEnum.FAILED.code());
        entity.setProcessStatus(PspCallbackProcessStatusEnum.FAILED.code());
        entity.setErrorMsg(StringUtils.left(ex.getMessage(), 1024));
        entity.setReceivedAt(Instant.now());
        entity.setProcessedAt(Instant.now());
        entity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        insert(entity);
        return entity;
    }

    /**
     * 完成回调日志处理状态。
     *
     * @param entity 回调日志实体
     * @param verifyStatus 验签状态
     * @param processStatus 业务处理状态
     * @param errorMsg 错误信息，成功时可为空
     */
    public void finish(PspCallbackLogEntity entity, String verifyStatus, String processStatus, String errorMsg) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        entity.setVerifyStatus(verifyStatus);
        entity.setProcessStatus(processStatus);
        entity.setErrorMsg(StringUtils.left(errorMsg, 1024));
        entity.setProcessedAt(Instant.now());
        pspCallbackLogService.updateById(entity);
    }

    /**
     * 插入回调日志。
     * <p>
     * 发生唯一键冲突时说明重复回调日志已存在，不再影响主流程。
     */
    private void insert(PspCallbackLogEntity entity) {
        try {
            pspCallbackLogService.insert(entity);
        } catch (DuplicateKeyException ignored) {
            // 置空 id，后续 finish 会直接跳过，避免重复回调覆盖已有日志。
            entity.setId(null);
        }
    }

    /**
     * 生成回调幂等键。
     * <p>
     * PSP 提供 callbackId 时优先使用；否则使用回调类型和 bodyHash 兜底。
     */
    private String callbackKey(PspCallbackLogEntity entity) {
        if (StringUtils.isNotBlank(entity.getCallbackId())) {
            return entity.getCallbackId();
        }
        return entity.getCallbackType() + ":" + entity.getBodyHash();
    }

    /**
     * 对象转 JSON。
     */
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
