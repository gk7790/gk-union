package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.OrderStatusLogDTO;
import com.gk.payment.entity.OrderStatusLogEntity;

public interface OrderStatusLogService extends CrudService<OrderStatusLogEntity, OrderStatusLogDTO> {

    /**
     * 记录订单状态变更；from/to 相同则跳过     *
     * @param orderType PAY / PAYOUT
     */
    void recordChange(String orderType,
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
                      String traceId);
}
