package com.gk.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.infra.enums.StatusEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.dao.LedgerEntryDao;
import com.gk.ledger.dao.LedgerHoldDao;
import com.gk.ledger.dao.LedgerJournalDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.entity.LedgerEntryEntity;
import com.gk.ledger.entity.LedgerHoldEntity;
import com.gk.ledger.entity.LedgerJournalEntity;
import com.gk.ledger.posting.MerchantBalanceAdjustPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerPostingServiceImplTest {
    @Mock
    private LedgerAccountDao ledgerAccountDao;
    @Mock
    private LedgerBalanceDao ledgerBalanceDao;
    @Mock
    private LedgerJournalDao ledgerJournalDao;
    @Mock
    private LedgerEntryDao ledgerEntryDao;
    @Mock
    private LedgerHoldDao ledgerHoldDao;
    @Mock
    private com.gk.ledger.service.LedgerAccountService ledgerAccountService;

    @InjectMocks
    private LedgerPostingServiceImpl ledgerPostingService;

    @Test
    void postMerchantBalanceRechargeCreditsMerchantAvailableFromManualSource() {
        when(ledgerJournalDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(ledgerAccountDao.selectOne(any(Wrapper.class)))
                .thenReturn(account(201L, "SYSTEM", 0L, "SYSTEM_CLEARING", "CREDIT"));
        when(ledgerAccountService.requireMerchantAccount(eq(10L), eq(20L), eq("MERCHANT_AVAILABLE"), eq("PHP")))
                .thenReturn(account(202L, SubjectTypeEnum.MERCHANT.code(), 20L, "MERCHANT_AVAILABLE", "CREDIT"));
        when(ledgerBalanceDao.selectOne(any(Wrapper.class))).thenAnswer(invocation -> balance());
        when(ledgerBalanceDao.applyEntry(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), anyLong(), anyString(), any(), anyInt())).thenReturn(1);
        AtomicLong ids = new AtomicLong(2000L);
        doAnswer(invocation -> {
            LedgerJournalEntity journal = invocation.getArgument(0);
            journal.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerJournalDao).insert(any(LedgerJournalEntity.class));
        doAnswer(invocation -> {
            LedgerEntryEntity entry = invocation.getArgument(0);
            entry.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerEntryDao).insert(any(LedgerEntryEntity.class));

        ledgerPostingService.postMerchantBalanceAdjust(balanceAdjustRequest("RECHARGE"));

        ArgumentCaptor<LedgerJournalEntity> journalCaptor = ArgumentCaptor.forClass(LedgerJournalEntity.class);
        verify(ledgerJournalDao).insert(journalCaptor.capture());
        assertThat(journalCaptor.getValue().getBizType()).isEqualTo("MERCHANT_BALANCE_ADJUST");
        assertThat(journalCaptor.getValue().getEventType()).isEqualTo("MANUAL_RECHARGE");
        assertThat(journalCaptor.getValue().getSourceType()).isEqualTo("MANUAL");

        ArgumentCaptor<Long> accountIds = ArgumentCaptor.forClass(Long.class);
        verify(ledgerBalanceDao, org.mockito.Mockito.times(2)).applyEntry(
                eq(10L),
                accountIds.capture(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyLong(),
                anyString(),
                any(),
                anyInt()
        );
        assertThat(accountIds.getAllValues()).containsExactly(201L, 202L);
    }

    @Test
    void postMerchantBalanceDeductDebitsMerchantAvailableFromManualSource() {
        when(ledgerJournalDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(ledgerAccountService.requireMerchantAccount(eq(10L), eq(20L), eq("MERCHANT_AVAILABLE"), eq("PHP")))
                .thenReturn(account(202L, SubjectTypeEnum.MERCHANT.code(), 20L, "MERCHANT_AVAILABLE", "CREDIT"));
        when(ledgerAccountDao.selectOne(any(Wrapper.class)))
                .thenReturn(account(201L, "SYSTEM", 0L, "SYSTEM_CLEARING", "CREDIT"));
        when(ledgerBalanceDao.selectOne(any(Wrapper.class))).thenAnswer(invocation -> balance());
        when(ledgerBalanceDao.applyEntry(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), anyLong(), anyString(), any(), anyInt())).thenReturn(1);
        AtomicLong ids = new AtomicLong(3000L);
        doAnswer(invocation -> {
            LedgerJournalEntity journal = invocation.getArgument(0);
            journal.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerJournalDao).insert(any(LedgerJournalEntity.class));
        doAnswer(invocation -> {
            LedgerEntryEntity entry = invocation.getArgument(0);
            entry.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerEntryDao).insert(any(LedgerEntryEntity.class));

        ledgerPostingService.postMerchantBalanceAdjust(balanceAdjustRequest("DEDUCT"));

        ArgumentCaptor<LedgerJournalEntity> journalCaptor = ArgumentCaptor.forClass(LedgerJournalEntity.class);
        verify(ledgerJournalDao).insert(journalCaptor.capture());
        assertThat(journalCaptor.getValue().getBizType()).isEqualTo("MERCHANT_BALANCE_ADJUST");
        assertThat(journalCaptor.getValue().getEventType()).isEqualTo("MANUAL_DEDUCT");
        assertThat(journalCaptor.getValue().getSourceType()).isEqualTo("MANUAL");

        ArgumentCaptor<Long> accountIds = ArgumentCaptor.forClass(Long.class);
        verify(ledgerBalanceDao, org.mockito.Mockito.times(2)).applyEntry(
                eq(10L),
                accountIds.capture(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyLong(),
                anyString(),
                any(),
                anyInt()
        );
        assertThat(accountIds.getAllValues()).containsExactly(202L, 201L);
    }

    @Test
    void postPayoutSuccessSplitsPrincipalAndMerchantFee() {
        when(ledgerJournalDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(ledgerHoldDao.selectOne(any(Wrapper.class))).thenReturn(holdingHold());
        when(ledgerAccountDao.selectById(101L)).thenReturn(account(101L, SubjectTypeEnum.MERCHANT.code(), 20L, "MERCHANT_FROZEN", "CREDIT"));
        when(ledgerAccountDao.selectOne(any(Wrapper.class)))
                .thenReturn(account(102L, "SYSTEM", 0L, "SYSTEM_CLEARING", "CREDIT"))
                .thenReturn(account(103L, SubjectTypeEnum.PLATFORM.code(), 0L, "PLATFORM_FEE_INCOME", "CREDIT"));
        when(ledgerBalanceDao.selectOne(any(Wrapper.class))).thenAnswer(invocation -> balance());
        when(ledgerBalanceDao.applyEntry(anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class),
                any(BigDecimal.class), anyLong(), anyString(), any(), anyInt())).thenReturn(1);
        AtomicLong ids = new AtomicLong(1000L);
        doAnswer(invocation -> {
            LedgerJournalEntity journal = invocation.getArgument(0);
            journal.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerJournalDao).insert(any(LedgerJournalEntity.class));
        doAnswer(invocation -> {
            LedgerEntryEntity entry = invocation.getArgument(0);
            entry.setId(ids.incrementAndGet());
            return 1;
        }).when(ledgerEntryDao).insert(any(LedgerEntryEntity.class));

        ledgerPostingService.postPayoutSuccess(payoutRequest());

        ArgumentCaptor<Long> accountIds = ArgumentCaptor.forClass(Long.class);
        verify(ledgerBalanceDao, org.mockito.Mockito.times(3)).applyEntry(
                eq(10L),
                accountIds.capture(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyLong(),
                anyString(),
                any(),
                anyInt()
        );
        assertThat(accountIds.getAllValues()).containsExactly(101L, 102L, 103L);
    }

    private MerchantBalanceAdjustPostingRequest balanceAdjustRequest(String adjustType) {
        MerchantBalanceAdjustPostingRequest request = new MerchantBalanceAdjustPostingRequest();
        request.setTenantId(10L);
        request.setMerchantId(20L);
        request.setBizId(40L);
        request.setAdjustOrderNo("MBA202406100001");
        request.setAdjustType(adjustType);
        request.setCurrency("PHP");
        request.setAmount(new BigDecimal("88.00000000"));
        request.setReason("manual test");
        return request;
    }

    private PayoutPostingRequest payoutRequest() {
        PayoutPostingRequest request = new PayoutPostingRequest();
        request.setTenantId(10L);
        request.setMerchantId(20L);
        request.setBizId(30L);
        request.setPayoutOrderNo("PO202406100001");
        request.setCurrency("PHP");
        request.setAmount(new BigDecimal("100.00000000"));
        request.setMerchantFeeAmount(new BigDecimal("5.00000000"));
        request.setTotalDebitAmount(new BigDecimal("105.00000000"));
        return request;
    }

    private LedgerHoldEntity holdingHold() {
        LedgerHoldEntity hold = new LedgerHoldEntity();
        hold.setId(1L);
        hold.setTenantId(10L);
        hold.setHoldNo("LH202406100001");
        hold.setFrozenAccountId(101L);
        hold.setAvailableAccountId(104L);
        hold.setBizNo("PO202406100001");
        hold.setRemainingAmount(new BigDecimal("105.00000000"));
        hold.setConsumedAmount(BigDecimal.ZERO);
        hold.setReleasedAmount(BigDecimal.ZERO);
        hold.setStatus("HOLDING");
        return hold;
    }

    private LedgerAccountEntity account(Long id, String ownerType, Long ownerId, String accountType, String normalSide) {
        LedgerAccountEntity account = new LedgerAccountEntity();
        account.setId(id);
        account.setTenantId(10L);
        account.setOwnerType(ownerType);
        account.setOwnerId(ownerId);
        account.setAccountType(accountType);
        account.setAccountNo(accountType + "-" + id);
        account.setCurrency("PHP");
        account.setNormalSide(normalSide);
        account.setAllowNegative(0);
        account.setStatus(StatusEnum.NORMAL.code());
        return account;
    }

    private LedgerBalanceEntity balance() {
        LedgerBalanceEntity balance = new LedgerBalanceEntity();
        balance.setBalance(new BigDecimal("1000.00000000"));
        balance.setDebitTotal(BigDecimal.ZERO);
        balance.setCreditTotal(BigDecimal.ZERO);
        balance.setVersion(0);
        return balance;
    }
}
