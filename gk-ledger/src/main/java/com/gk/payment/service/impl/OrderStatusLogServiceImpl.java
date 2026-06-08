package com.gk.payment.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.core.service.impl.CrudServiceImpl;
import com.gk.common.model.DynMap;
import com.gk.payment.dao.OrderStatusLogDao;
import com.gk.payment.dto.OrderStatusLogDTO;
import com.gk.payment.entity.OrderStatusLogEntity;
import com.gk.payment.service.OrderStatusLogService;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusLogServiceImpl extends CrudServiceImpl<OrderStatusLogDao, OrderStatusLogEntity, OrderStatusLogDTO> implements OrderStatusLogService {

    @Override
    public QueryWrapper<OrderStatusLogEntity> getWrapper(DynMap params) {
        QueryWrapper<OrderStatusLogEntity> wrapper = new QueryWrapper<>();
        Long tenantId = params.getLong("tenantId", null);
        Long merchantId = params.getLong("merchantId", null);
        Long orderId = params.getLong("orderId", null);
        String logNo = params.getStr("logNo");
        String orderType = params.getStr("orderType");
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
        wrapper.eq(StrUtil.isNotBlank(orderType), "order_type", orderType);
        wrapper.eq(StrUtil.isNotBlank(orderNo), "order_no", orderNo);
        wrapper.eq(StrUtil.isNotBlank(toStatus), "to_status", toStatus);
        wrapper.eq(StrUtil.isNotBlank(eventType), "event_type", eventType);
        wrapper.eq(StrUtil.isNotBlank(operatorType), "operator_type", operatorType);
        wrapper.eq(StrUtil.isNotBlank(requestId), "request_id", requestId);
        wrapper.eq(StrUtil.isNotBlank(traceId), "trace_id", traceId);
        return wrapper;
    }
}
