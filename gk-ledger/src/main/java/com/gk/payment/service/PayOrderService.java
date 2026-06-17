package com.gk.payment.service;

import com.gk.common.core.service.CrudService;
import com.gk.payment.dto.PayOrderDTO;
import com.gk.payment.entity.PayOrderEntity;

public interface PayOrderService extends CrudService<PayOrderEntity, PayOrderDTO> {

    /** 代收成功入账后：写入计划释放时间，T0/AUTO 且已到期则立即释放。 */
    void onPaySuccessPosted(Long orderId);

    /** 单笔释放待结算至可用（运营手动 / 内部调用）。 */
    void releaseSettle(Long orderId);

    /** 扫描到期 AUTO 订单并释放，供定时任务调用。 */
    int drainDueSettlements();

    /** 扫描超时未支付订单并关闭，供定时任务调用。 */
    int drainExpiredPayOrders();

    /** 扫描长时间处理中的代收订单并转人工处理，供定时任务调用。 */
    int drainLongProcessingOrders();
}
