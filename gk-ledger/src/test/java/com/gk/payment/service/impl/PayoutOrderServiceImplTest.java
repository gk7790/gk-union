package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gk.payment.dao.PayoutOrderDao;
import com.gk.payment.entity.PayoutOrderEntity;
import com.gk.payment.enums.PayoutOrderStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutOrderServiceImplTest {
    @Mock
    private PayoutOrderDao payoutOrderDao;
    @Mock
    private OrderStatusLogService orderStatusLogService;

    private PayoutOrderServiceImpl payoutOrderService;

    @BeforeEach
    void setUp() {
        payoutOrderService = new PayoutOrderServiceImpl(orderStatusLogService);
        payoutOrderService.setBaseDao(payoutOrderDao);
    }

    @Test
    void drainLongProcessingOrdersMovesOrderToManualReview() {
        PayoutOrderEntity order = new PayoutOrderEntity();
        order.setId(200L);
        order.setTenantId(10L);
        order.setMerchantId(20L);
        order.setPayoutOrderNo("PO200");
        order.setMerchantOrderNo("M200");
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setSubmittedAt(Instant.now().minusSeconds(3 * 60 * 60));
        order.setQueryCount(30);

        when(payoutOrderDao.selectList(any(Wrapper.class))).thenReturn(List.of(order));
        when(payoutOrderDao.update(isNull(), any(Wrapper.class))).thenReturn(1);

        int marked = payoutOrderService.drainLongProcessingOrders();

        assertThat(marked).isEqualTo(1);
        verify(payoutOrderDao).update(isNull(), any(Wrapper.class));
        verify(orderStatusLogService).recordChange(
                eq("PAYOUT"),
                eq(10L),
                eq(20L),
                eq(200L),
                eq("PO200"),
                eq(PayoutOrderStatusEnum.PROCESSING.code()),
                eq(PayoutOrderStatusEnum.MANUAL_REVIEW.code()),
                eq("PAYOUT_MANUAL_REVIEW"),
                eq("Payout order exceeded active query limit or SLA"),
                eq("SYSTEM"),
                isNull(),
                eq("M200"),
                isNull()
        );
    }
}
