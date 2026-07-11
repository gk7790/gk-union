package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.domain.key.BizKeyUtils;
import com.gk.payment.dao.OrderStatusLogDao;
import com.gk.payment.dto.OrderStatusLogDTO;
import com.gk.payment.entity.OrderStatusLogEntity;
import com.gk.payment.service.OrderStatusLogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusLogServiceImpl extends CrudServiceImpl<OrderStatusLogDao, OrderStatusLogEntity, OrderStatusLogDTO> implements OrderStatusLogService {

    @Override
    public void recordChange(String direction,
                             Long tenantId,
                             Long merchantId,
                             Long orderId,
                             String orderNo,
                             String fromStatus,
                             String toStatus,
                             String eventType,
                             String reason,
                             String operatorType,
                             String operatorId,
                             String requestId,
                             String traceId) {
        if (tenantId == null || orderId == null || StringUtils.isBlank(orderNo) || StringUtils.isBlank(toStatus)) {
            return;
        }
        if (StringUtils.equals(fromStatus, toStatus)) {
            return;
        }
        OrderStatusLogEntity entity = new OrderStatusLogEntity();
        entity.setTenantId(tenantId);
        entity.setMerchantId(merchantId);
        entity.setLogNo(BizKeyUtils.genOrderStatusLogNo());
        entity.setDirection(direction);
        entity.setOrderId(orderId);
        entity.setOrderNo(orderNo);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setEventType(StringUtils.defaultIfBlank(eventType, toStatus));
        entity.setReason(StringUtils.left(reason, 512));
        entity.setOperatorType(StringUtils.defaultIfBlank(operatorType, "SYSTEM"));
        entity.setOperatorId(operatorId);
        entity.setRequestId(requestId);
        entity.setTraceId(traceId);
        baseDao.insert(entity);
    }

    @Override
    public QueryWrapper<OrderStatusLogEntity> getWrapper(DynMap params) {
        QueryWrapper<OrderStatusLogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long orderId = params.getLong("orderId", null);
        String logNo = params.getStr("logNo");
        String direction = params.getStr("direction");
        String orderNo = params.getStr("orderNo");
        String toStatus = params.getStr("toStatus");
        String eventType = params.getStr("eventType");
        String operatorType = params.getStr("operatorType");
        String requestId = params.getStr("requestId");
        String traceId = params.getStr("traceId");

        wrapper.eq(tenantId != null, "tenant_id", tenantId);
        wrapper.eq(merchantId != null, "merchant_id", merchantId);
        wrapper.eq(orderId != null, "order_id", orderId);
        wrapper.eq(StrUtil.isNotBlank(logNo), "log_no", logNo);
        wrapper.eq(StrUtil.isNotBlank(direction), "direction", direction);
        wrapper.eq(StrUtil.isNotBlank(orderNo), "order_no", orderNo);
        wrapper.eq(StrUtil.isNotBlank(toStatus), "to_status", toStatus);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        wrapper.eq(StrUtil.isNotBlank(operatorType), "operator_type", operatorType);
        wrapper.eq(StrUtil.isNotBlank(requestId), "request_id", requestId);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }
}
