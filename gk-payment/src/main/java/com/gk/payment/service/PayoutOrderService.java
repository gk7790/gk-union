package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.PayoutOrderDTO;
import com.gk.payment.entity.PayoutOrderEntity;

public interface PayoutOrderService extends CrudService<PayoutOrderEntity, PayoutOrderDTO> {
    /** 扫描长时间处理中代付订单并转人工处理，供定时任务调用*/
    int drainLongProcessingOrders();
}
