package com.gk.payment.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.payment.dao.PayOrderDao;
import com.gk.payment.payout.dao.PayoutOrderDao;
import com.gk.payment.payout.entity.PayoutOrderEntity;
import com.gk.payment.payout.enums.PayoutOrderStatusEnum;
import com.gk.psp.query.PspOrderQueryResult;
import com.gk.psp.query.PspOrderResultHandler;
import com.gk.psp.query.PspPayQueryService;
import com.gk.psp.query.PspPayoutQueryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PspOrderQueryExecutorTest {

    @Test
    void drainsDuePayoutOrdersThroughPspQueryAndResultHandler() {
        PayOrderDao payOrderDao = mock(PayOrderDao.class);
        PayoutOrderDao payoutOrderDao = mock(PayoutOrderDao.class);
        PspPayQueryService payQueryService = mock(PspPayQueryService.class);
        PspPayoutQueryService payoutQueryService = mock(PspPayoutQueryService.class);
        PspOrderResultHandler resultHandler = mock(PspOrderResultHandler.class);
        PayoutOrderEntity order = payoutOrder();
        PspOrderQueryResult result = PspOrderQueryResult.builder()
                .success(true)
                .pspCode("WORLD")
                .systemOrderNo(order.getPayoutOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .pspOrderNo(order.getPspOrderNo())
                .pspStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .orderStatus(PayoutOrderStatusEnum.PROCESSING.code())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .build();

        when(payoutOrderDao.selectList(any(QueryWrapper.class))).thenReturn(List.of(order), List.of());
        when(payoutQueryService.query(order)).thenReturn(result);

        PspOrderQueryExecutor executor = new PspOrderQueryExecutor(
                payOrderDao,
                payoutOrderDao,
                payQueryService,
                payoutQueryService,
                resultHandler
        );

        int handled = executor.drainPayoutOrders();

        assertThat(handled).isEqualTo(1);
        verify(payoutQueryService).query(order);
        verify(resultHandler).handle(any(), any(), any());
    }

    private PayoutOrderEntity payoutOrder() {
        PayoutOrderEntity order = new PayoutOrderEntity();
        order.setId(100L);
        order.setTenantId(1L);
        order.setMerchantId(2L);
        order.setMerchantNo("M202606130001");
        order.setMerchantAppId(3L);
        order.setAppId("app_001");
        order.setPayoutOrderNo("POUT202606130001");
        order.setMerchantOrderNo("MO202606130001");
        order.setStatus(PayoutOrderStatusEnum.PROCESSING.code());
        order.setPspId(4L);
        order.setPspCode("WORLD");
        order.setPspAccountId(5L);
        order.setPspOrderNo("PSP202606130001");
        order.setAmount(new BigDecimal("100.00"));
        order.setMerchantFeeAmount(new BigDecimal("2.00"));
        order.setTotalDebitAmount(new BigDecimal("102.00"));
        order.setCurrency("PHP");
        order.setNextQueryAt(Instant.now().minusSeconds(1));
        order.setQueryCount(0);
        return order;
    }
}
