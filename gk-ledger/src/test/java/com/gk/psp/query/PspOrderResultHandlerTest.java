package com.gk.psp.query;

import com.gk.common.enums.BizTypeEnum;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.payment.enums.PayOrderStatusEnum;
import com.gk.payment.service.PayOrderService;
import com.gk.psp.callback.model.PspCallbackOrder;
import com.gk.psp.callback.support.PspCallbackNotifyCreator;
import com.gk.psp.callback.support.PspCallbackOrderProcessor;
import com.gk.psp.callback.support.PspCallbackValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PspOrderResultHandlerTest {

    @Test
    void postsLedgerAndCreatesNotifyForTerminalPayoutQuerySuccess() {
        PspCallbackOrderProcessor orderProcessor = mock(PspCallbackOrderProcessor.class);
        PspCallbackValidator validator = mock(PspCallbackValidator.class);
        LedgerPostingService ledgerPostingService = mock(LedgerPostingService.class);
        PspCallbackNotifyCreator notifyCreator = mock(PspCallbackNotifyCreator.class);
        PspOrderResultHandler handler = new PspOrderResultHandler(orderProcessor, validator, ledgerPostingService, notifyCreator, mock(PayOrderService.class));
        PspCallbackOrder order = payoutOrder();
        PspOrderQueryResult result = PspOrderQueryResult.builder()
                .pspCode("WORLD")
                .systemOrderNo(order.orderNo())
                .merchantOrderNo(order.merchantOrderNo())
                .pspOrderNo(order.pspOrderNo())
                .pspStatus("COMPLETED")
                .orderStatus(PayOrderStatusEnum.SUCCESS.code())
                .amount(order.amount())
                .currency(order.currency())
                .build();

        when(orderProcessor.process(eq(BizTypeEnum.PAYOUT_ORDER.code()), any(), eq(order), eq(null))).thenReturn(true);
        when(ledgerPostingService.postPayoutSuccess(any())).thenReturn(LedgerPostingResult.posted("J202606130001", "H202606130001"));

        handler.handle(BizTypeEnum.PAYOUT_ORDER.code(), order, result);

        verify(validator).validateTerminalCallback(eq(BizTypeEnum.PAYOUT_ORDER.code()), any(), eq(order));
        verify(ledgerPostingService).postPayoutSuccess(any());
        verify(orderProcessor).attachPostingResult(eq(BizTypeEnum.PAYOUT_ORDER.code()), eq(order.id()), eq(PayOrderStatusEnum.SUCCESS.code()), any());
        verify(notifyCreator).create(eq(BizTypeEnum.PAYOUT_ORDER.code()), any(), eq(order), eq(null));
    }

    @Test
    void doesNotPostLedgerOrNotifyForProcessingQueryResult() {
        PspCallbackOrderProcessor orderProcessor = mock(PspCallbackOrderProcessor.class);
        PspCallbackValidator validator = mock(PspCallbackValidator.class);
        LedgerPostingService ledgerPostingService = mock(LedgerPostingService.class);
        PspCallbackNotifyCreator notifyCreator = mock(PspCallbackNotifyCreator.class);
        PspOrderResultHandler handler = new PspOrderResultHandler(orderProcessor, validator, ledgerPostingService, notifyCreator, mock(PayOrderService.class));
        PspCallbackOrder order = payoutOrder();
        PspOrderQueryResult result = PspOrderQueryResult.builder()
                .pspCode("WORLD")
                .systemOrderNo(order.orderNo())
                .merchantOrderNo(order.merchantOrderNo())
                .pspOrderNo(order.pspOrderNo())
                .pspStatus("PENDING")
                .orderStatus(PayOrderStatusEnum.PROCESSING.code())
                .amount(order.amount())
                .currency(order.currency())
                .build();

        when(orderProcessor.process(eq(BizTypeEnum.PAYOUT_ORDER.code()), any(), eq(order), eq(null))).thenReturn(true);

        handler.handle(BizTypeEnum.PAYOUT_ORDER.code(), order, result);

        verify(validator, never()).validateTerminalCallback(any(), any(), any());
        verify(ledgerPostingService, never()).postPayoutSuccess(any());
        verify(ledgerPostingService, never()).releasePayout(any());
        verify(notifyCreator, never()).create(any(), any(), any(), any());
    }

    private PspCallbackOrder payoutOrder() {
        return new PspCallbackOrder(
                100L,
                1L,
                2L,
                "M202606130001",
                3L,
                "app_001",
                4L,
                "WORLD",
                5L,
                "secret",
                "POUT202606130001",
                "MO202606130001",
                "PSP202606130001",
                PayOrderStatusEnum.PROCESSING.code(),
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                null,
                new BigDecimal("102.00"),
                "PHP",
                "https://merchant.example/notify"
        );
    }
}
