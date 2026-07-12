package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.MerchantNotifyRecordDao;
import com.gk.payment.dto.MerchantNotifyRecordDTO;
import com.gk.payment.entity.MerchantNotifyRecordEntity;
import com.gk.payment.service.MerchantNotifyRecordService;
import org.springframework.stereotype.Service;

@Service
public class MerchantNotifyRecordServiceImpl extends CrudServiceImpl<MerchantNotifyRecordDao, MerchantNotifyRecordEntity, MerchantNotifyRecordDTO> implements MerchantNotifyRecordService {

    @Override
    public QueryWrapper<MerchantNotifyRecordEntity> getWrapper(DynMap params) {
        QueryWrapper<MerchantNotifyRecordEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long notifyTaskId = params.getLong("notifyTaskId", null);
        Integer attemptNo = params.containsKey("attemptNo") ? params.getInt("attemptNo") : null;
        Integer success = params.containsKey("success") ? params.getInt("success") : null;
        Integer responseStatus = params.containsKey("responseStatus") ? params.getInt("responseStatus") : null;
        String taskNo = params.getStr("taskNo");
        String contentType = params.getStr("contentType");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(notifyTaskId != null, "notify_task_id", notifyTaskId);
        wrapper.eq(attemptNo != null, "attempt_no", attemptNo);
        wrapper.eq(success != null, "success", success);
        wrapper.eq(responseStatus != null, "response_status", responseStatus);
        wrapper.eq(StrUtil.isNotBlank(taskNo), "task_no", taskNo);
        wrapper.eq(StrUtil.isNotBlank(contentType), "content_type", contentType);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }
}
