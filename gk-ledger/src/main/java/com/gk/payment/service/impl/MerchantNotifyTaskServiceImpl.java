package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.MerchantNotifyTaskDao;
import com.gk.payment.dto.MerchantNotifyTaskDTO;
import com.gk.payment.entity.MerchantNotifyTaskEntity;
import com.gk.payment.service.MerchantNotifyTaskService;
import org.springframework.stereotype.Service;

@Service
public class MerchantNotifyTaskServiceImpl extends CrudServiceImpl<MerchantNotifyTaskDao, MerchantNotifyTaskEntity, MerchantNotifyTaskDTO> implements MerchantNotifyTaskService {

    @Override
    public QueryWrapper<MerchantNotifyTaskEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantNotifyTaskEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long merchantAppId = params.getLong("merchantAppId", null);
        Long bizId = params.getLong("bizId", null);
        String taskNo = params.getStr("taskNo");
        String bizType = params.getStr("bizType");
        String bizNo = params.getStr("bizNo");
        String eventType = params.getStr("eventType");
        String sourceEventId = params.getStr("sourceEventId");
        String status = params.getStr("status");
        String payloadHash = params.getStr("payloadHash");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(merchantAppId != null, "merchant_app_id", merchantAppId);
        wrapper.eq(bizId != null, "biz_id", bizId);
        wrapper.eq(StrUtil.isNotBlank(taskNo), "task_no", taskNo);
        wrapper.eq(StrUtil.isNotBlank(bizType), "biz_type", bizType);
        wrapper.eq(StrUtil.isNotBlank(bizNo), "biz_no", bizNo);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        wrapper.eq(StrUtil.isNotBlank(sourceEventId), "source_event_id", sourceEventId);
        wrapper.eq(StrUtil.isNotBlank(status), "status", status);
        wrapper.eq(StrUtil.isNotBlank(payloadHash), "payload_hash", payloadHash);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }
}
