package com.gk.psp.callback.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.model.PspCallbackRequest;
import com.gk.psp.callback.model.PspCallbackResult;
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

@Component
@RequiredArgsConstructor
public class PspCallbackLogRecorder {
    private final PspCallbackLogService pspCallbackLogService;
    private final PspProviderDao pspProviderDao;
    private final ObjectMapper objectMapper;

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
        entity.setVerifyStatus("INIT");
        entity.setProcessStatus("INIT");
        entity.setReceivedAt(Instant.now());
        entity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        insert(entity);
        return entity;
    }

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
        entity.setVerifyStatus("FAILED");
        entity.setProcessStatus(PspCallbackConstants.PROCESS_FAILED);
        entity.setErrorMsg(StringUtils.left(ex.getMessage(), 1024));
        entity.setReceivedAt(Instant.now());
        entity.setProcessedAt(Instant.now());
        entity.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        insert(entity);
        return entity;
    }

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

    private void insert(PspCallbackLogEntity entity) {
        try {
            pspCallbackLogService.insert(entity);
        } catch (DuplicateKeyException ignored) {
            entity.setId(null);
        }
    }

    private String callbackKey(PspCallbackLogEntity entity) {
        if (StringUtils.isNotBlank(entity.getCallbackId())) {
            return entity.getCallbackId();
        }
        return entity.getCallbackType() + ":" + entity.getBodyHash();
    }

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
