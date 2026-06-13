package com.gk.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gk.ledger.dao.LedgerHoldDao;
import com.gk.ledger.entity.LedgerHoldEntity;
import com.gk.ledger.enums.LedgerHoldStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerHoldServiceImplTest {
    @Mock
    private LedgerHoldDao ledgerHoldDao;

    private LedgerHoldServiceImpl ledgerHoldService;

    @BeforeEach
    void setUp() {
        ledgerHoldService = new LedgerHoldServiceImpl();
        ledgerHoldService.setBaseDao(ledgerHoldDao);
    }

    @Test
    void drainExpiredHoldsMarksNonOrderHoldExpiredAndSkipsOrderHold() {
        LedgerHoldEntity riskHold = new LedgerHoldEntity();
        riskHold.setId(300L);
        riskHold.setHoldNo("H300");
        riskHold.setHoldScope("RISK");
        riskHold.setStatus(LedgerHoldStatusEnum.HOLDING.code());
        riskHold.setExpiredAt(Instant.now().minusSeconds(60));

        LedgerHoldEntity orderHold = new LedgerHoldEntity();
        orderHold.setId(301L);
        orderHold.setHoldNo("H301");
        orderHold.setHoldScope("ORDER");
        orderHold.setStatus(LedgerHoldStatusEnum.HOLDING.code());
        orderHold.setExpiredAt(Instant.now().minusSeconds(60));

        when(ledgerHoldDao.selectList(any(Wrapper.class))).thenReturn(List.of(riskHold, orderHold));
        when(ledgerHoldDao.update(isNull(), any(Wrapper.class))).thenReturn(1);

        int expired = ledgerHoldService.drainExpiredHolds();

        assertThat(expired).isEqualTo(1);
        verify(ledgerHoldDao, times(1)).update(isNull(), any(Wrapper.class));
    }
}
