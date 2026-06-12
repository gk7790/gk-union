package com.gk.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.utils.BizKeyUtils;
import com.gk.ledger.enums.LedgerAccountTypeEnum;
import com.gk.ledger.enums.LedgerDirectionEnum;
import com.gk.ledger.enums.LedgerHoldStatusEnum;
import com.gk.ledger.enums.LedgerJournalSourceEnum;
import com.gk.ledger.enums.LedgerJournalStatusEnum;
import com.gk.ledger.enums.LedgerOwnerTypeEnum;
import com.gk.ledger.enums.LedgerPostingEventEnum;
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
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.ledger.service.LedgerPostingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LedgerPostingServiceImpl implements LedgerPostingService {
    private static final int MONEY_SCALE = 8;
    private static final int STATUS_ENABLED = 1;

    private final LedgerAccountDao ledgerAccountDao;
    private final LedgerBalanceDao ledgerBalanceDao;
    private final LedgerJournalDao ledgerJournalDao;
    private final LedgerEntryDao ledgerEntryDao;
    private final LedgerHoldDao ledgerHoldDao;
    private final LedgerAccountService ledgerAccountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request) {
        validatePaySuccess(request);
        String eventType = LedgerPostingEventEnum.PAY_SUCCESS.code();
        LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType);
        if (existed != null) {
            return LedgerPostingResult.existed(existed.getJournalNo(), null);
        }

        BigDecimal settleAmount = amountOrDefault(request.getSettleAmount(), request.getAmount().subtract(defaultZero(request.getMerchantFeeAmount())));
        BigDecimal feeAmount = defaultZero(request.getMerchantFeeAmount());
        requireNonNegative(settleAmount, "settleAmount");
        requireNonNegative(feeAmount, "merchantFeeAmount");
        List<PostingLine> lines = new ArrayList<>();
        LedgerAccountEntity systemClearing = account(request.getTenantId(), LedgerOwnerTypeEnum.SYSTEM.code(), 0L, LedgerAccountTypeEnum.SYSTEM_CLEARING.code(), request.getCurrency());
        LedgerAccountEntity merchantAvailable = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.MERCHANT_AVAILABLE.code(), request.getCurrency());
        if (positive(settleAmount)) {
            lines.add(new PostingLine(systemClearing, LedgerDirectionEnum.DEBIT.code(), settleAmount, "Pay success settlement"));
            lines.add(new PostingLine(merchantAvailable, LedgerDirectionEnum.CREDIT.code(), settleAmount, "Pay success settlement"));
        }
        if (positive(feeAmount)) {
            LedgerAccountEntity platformFee = account(request.getTenantId(), SubjectTypeEnum.PLATFORM.code(), 0L, LedgerAccountTypeEnum.PLATFORM_FEE_INCOME.code(), request.getCurrency());
            lines.add(new PostingLine(systemClearing, LedgerDirectionEnum.DEBIT.code(), feeAmount, "Pay success merchant fee"));
            lines.add(new PostingLine(platformFee, LedgerDirectionEnum.CREDIT.code(), feeAmount, "Pay success merchant fee"));
        }

        LedgerJournalEntity journal = createJournal(
                request.getTenantId(),
                BizTypeEnum.PAY_ORDER.code(),
                request.getBizId(),
                request.getPayOrderNo(),
                eventType,
                request.getCurrency(),
                settleAmount.add(feeAmount),
                lines.size(),
                request.getTraceId(),
                "Pay success posting"
        );
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType, false);
        }
        postEntries(journal, lines);
        return LedgerPostingResult.posted(journal.getJournalNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult freezePayout(PayoutPostingRequest request) {
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_FREEZE.code();
        LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        if (existed != null) {
            LedgerHoldEntity hold = findHold(request.getTenantId(), request.getPayoutOrderNo());
            return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
        }

        BigDecimal totalDebitAmount = payoutTotalDebit(request);
        if (totalDebitAmount.compareTo(scale(request.getAmount())) < 0) {
            throw new IllegalArgumentException("Invalid payout posting request: totalDebitAmount must be greater than or equal to amount");
        }
        LedgerAccountEntity merchantAvailable = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.MERCHANT_AVAILABLE.code(), request.getCurrency());
        LedgerAccountEntity merchantFrozen = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.MERCHANT_FROZEN.code(), request.getCurrency());
        List<PostingLine> lines = List.of(
                new PostingLine(merchantAvailable, LedgerDirectionEnum.DEBIT.code(), totalDebitAmount, "Payout freeze"),
                new PostingLine(merchantFrozen, LedgerDirectionEnum.CREDIT.code(), totalDebitAmount, "Payout freeze")
        );

        LedgerJournalEntity journal = createJournal(
                request.getTenantId(),
                BizTypeEnum.PAYOUT_ORDER.code(),
                request.getBizId(),
                request.getPayoutOrderNo(),
                eventType,
                request.getCurrency(),
                totalDebitAmount,
                lines.size(),
                request.getTraceId(),
                "Payout freeze posting"
        );
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType, true);
        }
        postEntries(journal, lines);

        LedgerHoldEntity hold = new LedgerHoldEntity();
        hold.setTenantId(request.getTenantId());
        hold.setHoldNo(BizKeyUtils.genLedgerHoldNo());
        hold.setOwnerType(SubjectTypeEnum.MERCHANT.code());
        hold.setOwnerId(request.getMerchantId());
        hold.setCurrency(normalize(request.getCurrency()));
        hold.setAvailableAccountId(merchantAvailable.getId());
        hold.setAvailableAccountNo(merchantAvailable.getAccountNo());
        hold.setFrozenAccountId(merchantFrozen.getId());
        hold.setFrozenAccountNo(merchantFrozen.getAccountNo());
        hold.setBizType(BizTypeEnum.PAYOUT_ORDER.code());
        hold.setBizId(request.getBizId());
        hold.setBizNo(request.getPayoutOrderNo());
        hold.setHoldReason("PAYOUT");
        hold.setHoldScope("ORDER");
        hold.setHoldAmount(totalDebitAmount);
        hold.setReleasedAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setConsumedAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setRemainingAmount(totalDebitAmount);
        hold.setStatus(LedgerHoldStatusEnum.HOLDING.code());
        hold.setHoldJournalNo(journal.getJournalNo());
        ledgerHoldDao.insert(hold);
        return LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult postPayoutSuccess(PayoutPostingRequest request) {
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_SUCCESS.code();
        LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        if (existed != null) {
            LedgerHoldEntity hold = findHold(request.getTenantId(), request.getPayoutOrderNo());
            return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
        }
        LedgerHoldEntity hold = requireHoldingHold(request.getTenantId(), request.getPayoutOrderNo());

        BigDecimal totalDebitAmount = scale(hold.getRemainingAmount());
        BigDecimal payoutAmount = scale(request.getAmount());
        BigDecimal feeAmount = defaultZero(request.getMerchantFeeAmount());
        if (payoutAmount.add(feeAmount).compareTo(totalDebitAmount) != 0) {
            throw new IllegalStateException("Payout posting amount does not match hold amount: " + request.getPayoutOrderNo());
        }
        LedgerAccountEntity merchantFrozen = account(hold.getFrozenAccountId());
        LedgerAccountEntity systemClearing = account(request.getTenantId(), LedgerOwnerTypeEnum.SYSTEM.code(), 0L, LedgerAccountTypeEnum.SYSTEM_CLEARING.code(), request.getCurrency());
        List<PostingLine> lines = new ArrayList<>();
        lines.add(new PostingLine(merchantFrozen, LedgerDirectionEnum.DEBIT.code(), totalDebitAmount, "Payout consume frozen amount"));
        lines.add(new PostingLine(systemClearing, LedgerDirectionEnum.CREDIT.code(), payoutAmount, "Payout principal clearing"));
        if (positive(feeAmount)) {
            LedgerAccountEntity platformFee = account(request.getTenantId(), SubjectTypeEnum.PLATFORM.code(), 0L, LedgerAccountTypeEnum.PLATFORM_FEE_INCOME.code(), request.getCurrency());
            lines.add(new PostingLine(platformFee, LedgerDirectionEnum.CREDIT.code(), feeAmount, "Payout merchant fee income"));
        }
        LedgerJournalEntity journal = createJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getBizId(), request.getPayoutOrderNo(), eventType,
                request.getCurrency(), totalDebitAmount, lines.size(), request.getTraceId(), "Payout success posting");
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType, true);
        }
        postEntries(journal, lines);

        hold.setConsumedAmount(scale(defaultZero(hold.getConsumedAmount()).add(totalDebitAmount)));
        hold.setRemainingAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setStatus(LedgerHoldStatusEnum.CONSUMED.code());
        hold.setConsumeJournalNo(journal.getJournalNo());
        ledgerHoldDao.updateById(hold);
        return LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult releasePayout(PayoutPostingRequest request) {
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_FAILED.code();
        LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        if (existed != null) {
            LedgerHoldEntity hold = findHold(request.getTenantId(), request.getPayoutOrderNo());
            return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
        }
        LedgerHoldEntity hold = requireHoldingHold(request.getTenantId(), request.getPayoutOrderNo());

        BigDecimal amount = scale(hold.getRemainingAmount());
        LedgerAccountEntity merchantFrozen = account(hold.getFrozenAccountId());
        LedgerAccountEntity merchantAvailable = account(hold.getAvailableAccountId());
        List<PostingLine> lines = List.of(
                new PostingLine(merchantFrozen, LedgerDirectionEnum.DEBIT.code(), amount, "Payout release frozen amount"),
                new PostingLine(merchantAvailable, LedgerDirectionEnum.CREDIT.code(), amount, "Payout release frozen amount")
        );
        LedgerJournalEntity journal = createJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getBizId(), request.getPayoutOrderNo(), eventType,
                request.getCurrency(), amount, lines.size(), request.getTraceId(), "Payout failed release posting");
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType, true);
        }
        postEntries(journal, lines);

        hold.setReleasedAmount(scale(defaultZero(hold.getReleasedAmount()).add(amount)));
        hold.setRemainingAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setStatus(LedgerHoldStatusEnum.RELEASED.code());
        hold.setLastReleaseJournalNo(journal.getJournalNo());
        ledgerHoldDao.updateById(hold);
        return LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
    }

    private LedgerJournalEntity createJournal(Long tenantId, String bizType, Long bizId, String bizNo, String eventType,
                                              String currency, BigDecimal totalAmount, int entryCount, String traceId, String remark) {
        LedgerJournalEntity journal = new LedgerJournalEntity();
        journal.setTenantId(tenantId);
        journal.setJournalNo(BizKeyUtils.genLedgerJournalNo());
        journal.setBizType(bizType);
        journal.setBizId(bizId);
        journal.setBizNo(bizNo);
        journal.setEventType(eventType);
        journal.setCurrency(normalize(currency));
        journal.setTotalAmount(scale(totalAmount));
        journal.setEntryCount(entryCount);
        journal.setIdempotencyKey(idempotencyKey(bizNo, eventType));
        journal.setStatus(LedgerJournalStatusEnum.POSTED.code());
        journal.setSourceType(LedgerJournalSourceEnum.ORDER.code());
        journal.setTraceId(traceId);
        journal.setPostedAt(Instant.now());
        journal.setRemark(remark);
        try {
            ledgerJournalDao.insert(journal);
        } catch (DuplicateKeyException ex) {
            return null;
        }
        return journal;
    }

    private LedgerPostingResult existingPostingResult(Long tenantId, String bizType, String bizNo, String eventType, boolean withHold) {
        LedgerJournalEntity existed = findJournal(tenantId, bizType, bizNo, eventType);
        if (existed == null) {
            throw new IllegalStateException("Ledger journal idempotency conflict: " + bizNo + "/" + eventType);
        }
        LedgerHoldEntity hold = withHold ? findHold(tenantId, bizNo) : null;
        return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
    }

    private void postEntries(LedgerJournalEntity journal, List<PostingLine> lines) {
        int entryNo = 1;
        for (PostingLine line : lines) {
            if (!positive(line.amount())) {
                continue;
            }
            LedgerBalanceEntity balance = lockBalance(line.account());
            BigDecimal before = scale(balance.getBalance());
            BigDecimal change = balanceChange(line.account(), line.direction(), line.amount());
            BigDecimal after = scale(before.add(change));

            LedgerEntryEntity entry = new LedgerEntryEntity();
            entry.setTenantId(journal.getTenantId());
            entry.setJournalId(journal.getId());
            entry.setJournalNo(journal.getJournalNo());
            entry.setEntryNo(entryNo++);
            entry.setAccountId(line.account().getId());
            entry.setAccountNo(line.account().getAccountNo());
            entry.setOwnerType(line.account().getOwnerType());
            entry.setOwnerId(line.account().getOwnerId());
            entry.setAccountType(line.account().getAccountType());
            entry.setCurrency(line.account().getCurrency());
            entry.setNormalSide(line.account().getNormalSide());
            entry.setDirection(line.direction());
            entry.setAmount(scale(line.amount()));
            entry.setBalanceChange(change);
            entry.setBalanceBefore(before);
            entry.setBalanceAfter(after);
            entry.setBizType(journal.getBizType());
            entry.setBizId(journal.getBizId());
            entry.setBizNo(journal.getBizNo());
            entry.setEventType(journal.getEventType());
            entry.setSummary(line.summary());
            ledgerEntryDao.insert(entry);

            int updated = ledgerBalanceDao.applyEntry(
                    journal.getTenantId(),
                    line.account().getId(),
                    change,
                    LedgerDirectionEnum.DEBIT.code().equals(line.direction()) ? scale(line.amount()) : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    LedgerDirectionEnum.CREDIT.code().equals(line.direction()) ? scale(line.amount()) : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    entry.getId(),
                    journal.getJournalNo(),
                    journal.getPostedAt(),
                    line.account().getAllowNegative()
            );
            if (updated != 1) {
                throw new IllegalStateException("Insufficient ledger balance: " + line.account().getAccountNo());
            }
        }
    }

    private LedgerBalanceEntity lockBalance(LedgerAccountEntity account) {
        ensureBalance(account);
        LedgerBalanceEntity balance = ledgerBalanceDao.selectOne(new QueryWrapper<LedgerBalanceEntity>()
                .eq("tenant_id", account.getTenantId())
                .eq("account_id", account.getId())
                .last("limit 1 for update"));
        if (balance == null) {
            throw new IllegalStateException("Ledger balance is not configured: " + account.getAccountNo());
        }
        return balance;
    }

    private void ensureBalance(LedgerAccountEntity account) {
        LedgerBalanceEntity existed = ledgerBalanceDao.selectOne(new QueryWrapper<LedgerBalanceEntity>()
                .eq("tenant_id", account.getTenantId())
                .eq("account_id", account.getId())
                .last("limit 1"));
        if (existed != null) {
            return;
        }
        LedgerBalanceEntity balance = new LedgerBalanceEntity();
        balance.setTenantId(account.getTenantId());
        balance.setAccountId(account.getId());
        balance.setAccountNo(account.getAccountNo());
        balance.setCurrency(account.getCurrency());
        balance.setBalance(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        balance.setDebitTotal(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        balance.setCreditTotal(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        balance.setVersion(0);
        try {
            ledgerBalanceDao.insert(balance);
        } catch (DuplicateKeyException ignored) {
            // Another transaction initialized the balance row first.
        }
    }

    private LedgerAccountEntity account(Long tenantId, String ownerType, Long ownerId, String accountType, String currency) {
        if (SubjectTypeEnum.MERCHANT.code().equals(ownerType)
                && (LedgerAccountTypeEnum.MERCHANT_AVAILABLE.matches(accountType) || LedgerAccountTypeEnum.MERCHANT_FROZEN.matches(accountType))) {
            return ledgerAccountService.requireMerchantAccount(tenantId, ownerId, accountType, currency);
        }
        LedgerAccountEntity account = ledgerAccountDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", ownerType)
                .eq("owner_id", ownerId)
                .eq("account_type", accountType)
                .eq("currency", normalize(currency))
                .eq("status", STATUS_ENABLED)
                .last("limit 1"));
        if (account == null) {
            throw new IllegalStateException("Ledger account is not configured: " + ownerType + "/" + accountType + "/" + currency);
        }
        return account;
    }

    private LedgerAccountEntity account(Long accountId) {
        LedgerAccountEntity account = ledgerAccountDao.selectById(accountId);
        if (account == null || !Integer.valueOf(STATUS_ENABLED).equals(account.getStatus())) {
            throw new IllegalStateException("Ledger account is not available: " + accountId);
        }
        return account;
    }

    private LedgerJournalEntity findJournal(Long tenantId, String bizType, String bizNo, String eventType) {
        return ledgerJournalDao.selectOne(new QueryWrapper<LedgerJournalEntity>()
                .eq("tenant_id", tenantId)
                .eq("biz_type", bizType)
                .eq("biz_no", bizNo)
                .eq("event_type", eventType)
                .last("limit 1"));
    }

    private LedgerHoldEntity findHold(Long tenantId, String bizNo) {
        return ledgerHoldDao.selectOne(new QueryWrapper<LedgerHoldEntity>()
                .eq("tenant_id", tenantId)
                .eq("biz_type", BizTypeEnum.PAYOUT_ORDER.code())
                .eq("biz_no", bizNo)
                .last("limit 1"));
    }

    private LedgerHoldEntity requireHoldingHold(Long tenantId, String bizNo) {
        LedgerHoldEntity hold = ledgerHoldDao.selectOne(new QueryWrapper<LedgerHoldEntity>()
                .eq("tenant_id", tenantId)
                .eq("biz_type", BizTypeEnum.PAYOUT_ORDER.code())
                .eq("biz_no", bizNo)
                .in("status", LedgerHoldStatusEnum.HOLDING.code())
                .last("limit 1 for update"));
        if (hold == null) {
            throw new IllegalStateException("Ledger hold is not holding: " + bizNo);
        }
        if (!positive(hold.getRemainingAmount())) {
            throw new IllegalStateException("Ledger hold remaining amount is zero: " + bizNo);
        }
        return hold;
    }

    private BigDecimal balanceChange(LedgerAccountEntity account, String direction, BigDecimal amount) {
        BigDecimal scaled = scale(amount);
        return StringUtils.equalsIgnoreCase(account.getNormalSide(), direction) ? scaled : scaled.negate();
    }

    private String idempotencyKey(String bizNo, String eventType) {
        return bizNo + ":" + eventType;
    }

    private BigDecimal payoutTotalDebit(PayoutPostingRequest request) {
        return amountOrDefault(request.getTotalDebitAmount(), request.getAmount().add(defaultZero(request.getMerchantFeeAmount())));
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (scale(value).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid posting amount: " + field);
        }
    }

    private void validatePaySuccess(PaySuccessPostingRequest request) {
        if (request == null || request.getTenantId() == null || request.getMerchantId() == null || StringUtils.isBlank(request.getPayOrderNo())
                || StringUtils.isBlank(request.getCurrency()) || !positive(request.getAmount())) {
            throw new IllegalArgumentException("Invalid pay success posting request");
        }
    }

    private void validatePayout(PayoutPostingRequest request) {
        if (request == null || request.getTenantId() == null || request.getMerchantId() == null || StringUtils.isBlank(request.getPayoutOrderNo())
                || StringUtils.isBlank(request.getCurrency()) || !positive(request.getAmount())) {
            throw new IllegalArgumentException("Invalid payout posting request");
        }
        requireNonNegative(defaultZero(request.getMerchantFeeAmount()), "merchantFeeAmount");
        if (request.getTotalDebitAmount() != null && !positive(request.getTotalDebitAmount())) {
            throw new IllegalArgumentException("Invalid payout posting request: totalDebitAmount");
        }
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal amountOrDefault(BigDecimal value, BigDecimal fallback) {
        return scale(value == null ? fallback : value);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : scale(value);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private record PostingLine(LedgerAccountEntity account, String direction, BigDecimal amount, String summary) {
    }
}
