package com.gk.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.merchant.dao.MerchantDao;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.entity.PayOrderEntity;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.service.OrderStatusLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOrderServiceImplTest {
    @Mock
    private PayOrderDao payOrderDao;
    @Mock
    private MerchantDao merchantDao;
    @Mock
    private LedgerPostingService ledgerPostingService;
    @Mock
    private OrderStatusLogService orderStatusLogService;

    private PayOrderServiceImpl payOrderService;

    @BeforeEach
    void setUp() {
        payOrderService = new PayOrderServiceImpl(merchantDao, ledgerPostingService, orderStatusLogService);
        payOrderService.setBaseDao(payOrderDao);
    }

    @Test
    void drainExpiredPayOrdersClosesExpiredUnpaidOrder() {
        PayOrderEntity order = new PayOrderEntity();
        order.setId(100L);
        order.setTenantId(10L);
        order.setMerchantId(20L);
        order.setPayOrderNo("PAY100");
        order.setMerchantOrderNo("M100");
        order.setStatus(PayOrderStatusEnum.PROCESSING.code());
        order.setPaidAmount(BigDecimal.ZERO);
        order.setExpireAt(Instant.now().minusSeconds(60));

        when(payOrderDao.selectList(any(Wrapper.class))).thenReturn(List.of(order));
        when(payOrderDao.update(isNull(), any(Wrapper.class))).thenReturn(1);

        int closed = payOrderService.drainExpiredPayOrders();

        assertThat(closed).isEqualTo(1);
        verify(payOrderDao).update(isNull(), any(Wrapper.class));
        verify(orderStatusLogService).recordChange(
                eq("PAY"),
                eq(10L),
                eq(20L),
                eq(100L),
                eq("PAY100"),
                eq(PayOrderStatusEnum.PROCESSING.code()),
                eq(PayOrderStatusEnum.CLOSED.code()),
                eq("ORDER_EXPIRED"),
                eq("Pay order expired"),
                eq("SYSTEM"),
                isNull(),
                eq("M100"),
                isNull()
        );
    }
}
