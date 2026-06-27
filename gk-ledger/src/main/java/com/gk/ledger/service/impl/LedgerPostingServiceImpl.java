package com.gk.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.common.enums.BizTypeEnum;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.SubjectTypeEnum;
import com.gk.common.exception.GkException;
import com.gk.common.utils.BizKeyUtils;
import com.gk.infra.enums.StatusEnum;
import com.gk.infra.utils.AsynUtils;
import com.gk.ledger.enums.LedgerAccountTypeEnum;
import com.gk.ledger.enums.LedgerDirectionEnum;
import com.gk.ledger.enums.LedgerHoldStatusEnum;
import com.gk.ledger.enums.LedgerJournalSourceEnum;
import com.gk.ledger.enums.LedgerJournalStatusEnum;
import com.gk.ledger.enums.LedgerOwnerTypeEnum;
import com.gk.ledger.enums.LedgerPostingEventEnum;
import com.gk.adjustment.enums.MerchantBalanceAdjustTypeEnum;
import com.gk.ledger.dao.LedgerAccountDao;
import com.gk.ledger.dao.LedgerBalanceDao;
import com.gk.ledger.dao.LedgerEntryDao;
import com.gk.ledger.dao.LedgerHoldDao;
import com.gk.ledger.dao.LedgerJournalDao;
import com.gk.ledger.dao.MerchantWalletStatementDao;
import com.gk.ledger.entity.LedgerAccountEntity;
import com.gk.ledger.entity.LedgerBalanceEntity;
import com.gk.ledger.entity.LedgerEntryEntity;
import com.gk.ledger.entity.LedgerHoldEntity;
import com.gk.ledger.entity.LedgerJournalEntity;
import com.gk.ledger.entity.MerchantWalletStatementEntity;
import com.gk.ledger.exception.InsufficientLedgerBalanceException;
import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.MerchantBalanceAdjustPostingRequest;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;
import com.gk.ledger.service.LedgerAccountService;
import com.gk.ledger.service.LedgerPostingService;
import com.gk.ledger.enums.MerchantWalletStatementEffectEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账务入账服务 *
 * <p>本类负责把订单、结算、代付冻结、人工调账等业务事件转换ledger_journal * ledger_entry ledger_balance 的一致性变更。所有对外方法都要求幂等 * 同一业务单号 + 同一事件类型只能生成一张账务凭证/p>
 */
@Service
@Slf4j
public class LedgerPostingServiceImpl implements LedgerPostingService {
    private static final int MONEY_SCALE = 8;
    private static final long SLOW_LEDGER_PROFILE_MILLIS = 1000L;
    private static final long MERCHANT_ACCOUNT_CACHE_TTL_MILLIS = 60_000L;
    private static final int MERCHANT_ACCOUNT_CACHE_MAX_SIZE = 20_000;

    private final LedgerAccountDao ledgerAccountDao;
    private final LedgerBalanceDao ledgerBalanceDao;
    private final LedgerJournalDao ledgerJournalDao;
    private final LedgerEntryDao ledgerEntryDao;
    private final LedgerHoldDao ledgerHoldDao;
    private final LedgerAccountService ledgerAccountService;
    private final MerchantWalletStatementDao merchantWalletStatementDao;
    private final Map<MerchantAccountCacheKey, CachedLedgerAccount> merchantAccountCache = new ConcurrentHashMap<>();

    public LedgerPostingServiceImpl(LedgerAccountDao ledgerAccountDao,
                                    LedgerBalanceDao ledgerBalanceDao,
                                    LedgerJournalDao ledgerJournalDao,
                                    LedgerEntryDao ledgerEntryDao,
                                    LedgerHoldDao ledgerHoldDao,
                                    LedgerAccountService ledgerAccountService,
                                    MerchantWalletStatementDao merchantWalletStatementDao) {
        this.ledgerAccountDao = ledgerAccountDao;
        this.ledgerBalanceDao = ledgerBalanceDao;
        this.ledgerJournalDao = ledgerJournalDao;
        this.ledgerEntryDao = ledgerEntryDao;
        this.ledgerHoldDao = ledgerHoldDao;
        this.ledgerAccountService = ledgerAccountService;
        this.merchantWalletStatementDao = merchantWalletStatementDao;
    }

    /**
     * 代收成功入账     *
     * <p>资金进入 PSP 清算账，商户可结算金额进入待结算账户，商户手续费进入内部手续费收入账户/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request) {
        validatePaySuccess(request);
        String eventType = LedgerPostingEventEnum.PAY_SUCCESS.code();
        // 幂等检查：同一代收订单成功回调只能入账一次
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType);
        if (existed != null) {
            return LedgerPostingResult.existed(existed.getJournalNo(), null);
        }

        // 结算金额默认等于订单金额减商户手续费；手续费为空时按 0 处理
                BigDecimal settleAmount = amountOrDefault(request.getSettleAmount(), request.getAmount().subtract(defaultZero(request.getMerchantFeeAmount())));
        BigDecimal feeAmount = defaultZero(request.getMerchantFeeAmount());
        // PSP 清算侧收到的是订单总资金：商户待结算金+ 内部手续费收入
                BigDecimal clearingAmount = settleAmount.add(feeAmount);
        requireNonNegative(settleAmount, "settleAmount");
        requireNonNegative(feeAmount, "merchantFeeAmount");
        List<PostingLine> lines = new ArrayList<>();
        // pspAccountId 时走 PSP 清算账；历史/沙箱订单没有时回退内部清算账
                LedgerAccountEntity clearing = clearingAccount(request.getTenantId(), request.getPspAccountId(), request.getCurrency());
        LedgerAccountEntity merchantPending = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.PENDING_SETTLE.code(), request.getCurrency());
        if (positive(clearingAmount)) {
            lines.add(new PostingLine(clearing, LedgerDirectionEnum.DEBIT.code(), clearingAmount, "Pay success PSP clearing"));
        }
        if (positive(settleAmount)) {
            lines.add(new PostingLine(merchantPending, LedgerDirectionEnum.CREDIT.code(), settleAmount, "Pay success pending settlement"));
        }
        if (positive(feeAmount)) {
            LedgerAccountEntity internalFeeIncome = account(request.getTenantId(), LedgerOwnerTypeEnum.INTERNAL.code(), 0L, LedgerAccountTypeEnum.FEE_INCOME.code(), request.getCurrency());
            lines.add(new PostingLine(internalFeeIncome, LedgerDirectionEnum.CREDIT.code(), feeAmount, "Pay success merchant fee"));
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
            // 插入凭证时遇到唯一键冲突，说明并发线程已经完成入账，返回已有凭证
                        return existingPostingResult(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType, false);
        }
        // 真正ledger_entry 并更ledger_balance        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.pay(request, settleAmount, feeAmount));
        return LedgerPostingResult.posted(journal.getJournalNo());
    }

    /**
     * 释放代收待结算金额到商户可用余额     *
     * <p>通常由结算释放任务调用，PENDING_SETTLE 转到 AVAILABLE/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult releasePaySettle(PaySuccessPostingRequest request) {
        validatePaySuccess(request);
        String eventType = LedgerPostingEventEnum.SETTLE_RELEASE.code();
        // 幂等检查：同一代收订单只能释放一次同类型结算事件
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType);
        if (existed != null) {
            return LedgerPostingResult.existed(existed.getJournalNo(), null);
        }

        BigDecimal settleAmount = amountOrDefault(request.getSettleAmount(), request.getAmount().subtract(defaultZero(request.getMerchantFeeAmount())));
        requireNonNegative(settleAmount, "settleAmount");
        if (!positive(settleAmount)) {
            throw new GkException("Invalid settle release amount");
        }
        // 借：商户待结算；贷：商户可用
                LedgerAccountEntity merchantPending = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.PENDING_SETTLE.code(), request.getCurrency());
        LedgerAccountEntity merchantAvailable = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.AVAILABLE.code(), request.getCurrency());
        List<PostingLine> lines = List.of(
                new PostingLine(merchantPending, LedgerDirectionEnum.DEBIT.code(), settleAmount, "Settle release to available"),
                new PostingLine(merchantAvailable, LedgerDirectionEnum.CREDIT.code(), settleAmount, "Settle release to available")
        );

        LedgerJournalEntity journal = createJournal(
                request.getTenantId(),
                BizTypeEnum.PAY_ORDER.code(),
                request.getBizId(),
                request.getPayOrderNo(),
                eventType,
                request.getCurrency(),
                settleAmount,
                lines.size(),
                request.getTraceId(),
                "Pay settle release posting"
        );
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAY_ORDER.code(), request.getPayOrderNo(), eventType, false);
        }
        // 发布分录并原子更新两个账户余额        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.pay(request, settleAmount, defaultZero(request.getMerchantFeeAmount())));
        return LedgerPostingResult.posted(journal.getJournalNo());
    }

    /**
     * 代付下单冻结商户余额     *
     * <p>代付提交 PSP 前先把商户可用余额冻结，避免重复提交或余额被其他业务占用/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult freezePayout(PayoutPostingRequest request) {
        long profileStartNanos = System.nanoTime();
        long profileLastNanos = profileStartNanos;
        StringBuilder profileSteps = new StringBuilder();
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_FREEZE.code();
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "validate");
        /*
        // 幂等检查：重复冻结时返回已有凭证和 holdNo
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        // 冻结总额默认等于代付本金 + 商户手续费
                BigDecimal totalDebitAmount = payoutTotalDebit(request);
        */
        BigDecimal totalDebitAmount = payoutTotalDebit(request);
        if (totalDebitAmount.compareTo(scale(request.getAmount())) < 0) {
            throw new GkException("Invalid payout posting request: totalDebitAmount must be greater than or equal to amount");
        }
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "amount");
        // 借：商户可用；贷：商户冻结
                LedgerAccountEntity merchantAvailable = merchantAccountForPosting(request.getTenantId(), request.getMerchantId(), LedgerAccountTypeEnum.AVAILABLE.code(), request.getCurrency());
        LedgerAccountEntity merchantFrozen = merchantAccountForPosting(request.getTenantId(), request.getMerchantId(), LedgerAccountTypeEnum.FROZEN.code(), request.getCurrency());
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "load_accounts");
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
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "create_journal");
        if (journal == null) {
            LedgerPostingResult result = existingPostingResult(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType, true);
            markLedgerStep(profileSteps, profileLastNanos, "load_existing");
            logLedgerProfileIfSlow("freeze_payout", request, profileStartNanos, profileSteps, "EXISTED");
            return result;
        }
        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.payout(request, totalDebitAmount));
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "post_entries");

        // 冻结分录成功后记ledger_hold，后续成功消费或失败释放都以它为准
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
        markLedgerStep(profileSteps, profileLastNanos, "insert_hold");
        LedgerPostingResult result = LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
        logLedgerProfileIfSlow("freeze_payout", request, profileStartNanos, profileSteps, "POSTED");
        return result;
    }

    /**
     * 代付成功入账     *
     * <p>PSP 确认代付成功后，消费商户冻结金额，同时确PSP 清算侧出款和内部手续费收入/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult postPayoutSuccess(PayoutPostingRequest request) {
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_SUCCESS.code();
        // 幂等检查：同一代付订单成功回调只能消费冻结一次
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        if (existed != null) {
            LedgerHoldEntity hold = findHold(request.getTenantId(), request.getPayoutOrderNo());
            return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
        }
        // 成功扣款必须基于仍处HOLDING 状态的冻结记录
                LedgerHoldEntity hold = requireHoldingHold(request.getTenantId(), request.getPayoutOrderNo());

        BigDecimal totalDebitAmount = scale(hold.getRemainingAmount());
        BigDecimal payoutAmount = scale(request.getAmount());
        BigDecimal feeAmount = defaultZero(request.getMerchantFeeAmount());
        // 冻结金额必须刚好覆盖代付本金和商户手续费，防止错账
                if (payoutAmount.add(feeAmount).compareTo(totalDebitAmount) != 0) {
            throw new IllegalStateException("Payout posting amount does not match hold amount: " + request.getPayoutOrderNo());
        }
        // 借：商户冻结；贷：PSP 清算本金；贷：内部手续费收入
                LedgerAccountEntity merchantFrozen = account(hold.getFrozenAccountId());
        LedgerAccountEntity clearing = clearingAccount(request.getTenantId(), request.getPspAccountId(), request.getCurrency());
        List<PostingLine> lines = new ArrayList<>();
        lines.add(new PostingLine(merchantFrozen, LedgerDirectionEnum.DEBIT.code(), totalDebitAmount, "Payout consume frozen amount"));
        lines.add(new PostingLine(clearing, LedgerDirectionEnum.CREDIT.code(), payoutAmount, "Payout principal PSP clearing"));
        if (positive(feeAmount)) {
            LedgerAccountEntity internalFeeIncome = account(request.getTenantId(), LedgerOwnerTypeEnum.INTERNAL.code(), 0L, LedgerAccountTypeEnum.FEE_INCOME.code(), request.getCurrency());
            lines.add(new PostingLine(internalFeeIncome, LedgerDirectionEnum.CREDIT.code(), feeAmount, "Payout merchant fee income"));
        }
        LedgerJournalEntity journal = createJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getBizId(), request.getPayoutOrderNo(), eventType,
                request.getCurrency(), totalDebitAmount, lines.size(), request.getTraceId(), "Payout success posting");
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType, true);
        }
        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.payout(request, totalDebitAmount));

        // 分录落账后把冻结记录标记为已消费，防止后续再次释放        hold.setConsumedAmount(scale(defaultZero(hold.getConsumedAmount()).add(totalDebitAmount)));
        hold.setRemainingAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setStatus(LedgerHoldStatusEnum.CONSUMED.code());
        hold.setConsumeJournalNo(journal.getJournalNo());
        ledgerHoldDao.updateById(hold);
        return LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
    }

    /**
     * 代付失败释放冻结金额     *
     * <p>PSP 明确失败或提交失败时，把原冻结金额从商户冻结账户退回商户可用账户/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult releasePayout(PayoutPostingRequest request) {
        validatePayout(request);
        String eventType = LedgerPostingEventEnum.PAYOUT_FAILED.code();
        // 幂等检查：重复失败回调只返回已有释放凭证
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.PAYOUT_ORDER.code(), request.getPayoutOrderNo(), eventType);
        if (existed != null) {
            LedgerHoldEntity hold = findHold(request.getTenantId(), request.getPayoutOrderNo());
            return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
        }
        // 只有仍在冻结中的 hold 才允许释放
                LedgerHoldEntity hold = requireHoldingHold(request.getTenantId(), request.getPayoutOrderNo());

        BigDecimal amount = scale(hold.getRemainingAmount());
        // 借：商户冻结；贷：商户可用
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
        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.payout(request, amount));

        // 释放完成后清remainingAmount，并记录最后一次释放凭证号        hold.setReleasedAmount(scale(defaultZero(hold.getReleasedAmount()).add(amount)));
        hold.setRemainingAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        hold.setStatus(LedgerHoldStatusEnum.RELEASED.code());
        hold.setLastReleaseJournalNo(journal.getJournalNo());
        ledgerHoldDao.updateById(hold);
        return LedgerPostingResult.posted(journal.getJournalNo(), hold.getHoldNo());
    }

    /**
     * 商户余额人工调账     *
     * <p>支持运营手工增加或扣减商户可用余额，凭证来源标记MANUAL/p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerPostingResult postMerchantBalanceAdjust(MerchantBalanceAdjustPostingRequest request) {
        MerchantBalanceAdjustTypeEnum adjustType = validateMerchantBalanceAdjust(request);
        String eventType = adjustType.eventType();
        // 幂等检查：同一调账单同一事件类型只允许入账一次
                LedgerJournalEntity existed = findJournal(request.getTenantId(), BizTypeEnum.BALANCE_ADJUST.code(), request.getAdjustOrderNo(), eventType);
        if (existed != null) {
            return LedgerPostingResult.existed(existed.getJournalNo(), null);
        }

        BigDecimal amount = scale(request.getAmount());
        LedgerAccountEntity internalClearing = account(request.getTenantId(), LedgerOwnerTypeEnum.INTERNAL.code(), 0L, LedgerAccountTypeEnum.CLEARING.code(), request.getCurrency());
        LedgerAccountEntity merchantAvailable = account(request.getTenantId(), SubjectTypeEnum.MERCHANT.code(), request.getMerchantId(), LedgerAccountTypeEnum.AVAILABLE.code(), request.getCurrency());

        List<PostingLine> lines;
        if (adjustType.increase()) {
            // 增加余额：内部清算出资，商户可用增加
            lines = List.of(
                    new PostingLine(internalClearing, LedgerDirectionEnum.DEBIT.code(), amount, "Merchant balance manual increase"),
                    new PostingLine(merchantAvailable, LedgerDirectionEnum.CREDIT.code(), amount, "Merchant balance manual increase")
            );
        } else {
            // 扣减余额：商户可用减少，内部清算回收
            lines = List.of(
                    new PostingLine(merchantAvailable, LedgerDirectionEnum.DEBIT.code(), amount, "Merchant balance manual decrease"),
                    new PostingLine(internalClearing, LedgerDirectionEnum.CREDIT.code(), amount, "Merchant balance manual decrease")
            );
        }

        LedgerJournalEntity journal = createJournal(
                request.getTenantId(),
                BizTypeEnum.BALANCE_ADJUST.code(),
                request.getBizId(),
                request.getAdjustOrderNo(),
                eventType,
                request.getCurrency(),
                amount,
                lines.size(),
                request.getTraceId(),
                request.getReason(),
                LedgerJournalSourceEnum.MANUAL.code(),
                request.getReverseOfJournalNo()
        );
        if (journal == null) {
            return existingPostingResult(request.getTenantId(), BizTypeEnum.BALANCE_ADJUST.code(), request.getAdjustOrderNo(), eventType, false);
        }
        postEntriesOptimized(journal, lines, MerchantStatementSnapshot.adjust(request, amount));
        return LedgerPostingResult.posted(journal.getJournalNo());
    }

    /**
     * 创建订单来源的账务凭证     *
     * <p>默认 sourceType 使用 ORDER，适用于订单、回调、结算任务触发的入账/p>
     */
    private LedgerJournalEntity createJournal(Long tenantId, String bizType, Long bizId, String bizNo, String eventType,
                                              String currency, BigDecimal totalAmount, int entryCount, String traceId, String remark) {
        return createJournal(tenantId, bizType, bizId, bizNo, eventType, currency, totalAmount, entryCount, traceId, remark,
                LedgerJournalSourceEnum.ORDER.code(), null);
    }

    /**
     * 创建账务凭证主记录     *
     * <p>凭证唯一键由 bizNo + eventType 组成，并依赖数据库唯一索引兜住并发幂等/p>
     */
    private LedgerJournalEntity createJournal(Long tenantId, String bizType, Long bizId, String bizNo, String eventType,
                                              String currency, BigDecimal totalAmount, int entryCount, String traceId,
                                              String remark, String sourceType, String reverseOfJournalNo) {
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
        // 幂等键必须稳定，保证重复回调、重试任务不会重复入账        journal.setIdempotencyKey(idempotencyKey(bizNo, eventType));
        journal.setStatus(LedgerJournalStatusEnum.POSTED.code());
        journal.setSourceType(sourceType);
        journal.setReverseOfJournalNo(reverseOfJournalNo);
        journal.setTraceId(traceId);
        journal.setPostedAt(Instant.now());
        journal.setRemark(remark);
        try {
            ledgerJournalDao.insert(journal);
        } catch (DuplicateKeyException ex) {
            // 并发插入同一凭证时返null，由上层查询已有凭证
                        return null;
        }
        return journal;
    }

    /**
     * 根据幂等冲突返回已经存在的入账结果     *
     * <p>代付相关事件需要同时返holdNo，方便订单侧回填冻结或释放结果/p>
     */
    private LedgerPostingResult existingPostingResult(Long tenantId, String bizType, String bizNo, String eventType, boolean withHold) {
        LedgerJournalEntity existed = findJournal(tenantId, bizType, bizNo, eventType);
        if (existed == null) {
            throw new IllegalStateException("Ledger journal idempotency conflict: " + bizNo + "/" + eventType);
        }
        LedgerHoldEntity hold = withHold ? findHold(tenantId, bizNo) : null;
        return LedgerPostingResult.existed(existed.getJournalNo(), hold == null ? null : hold.getHoldNo());
    }

    /**
     * 写入凭证分录并更新账户余额     *
     * <p>每条 PostingLine 会生成一ledger_entry，再通过 ledgerBalanceDao.applyEntry 原子更新余额/p>
     */
    private void postEntries(LedgerJournalEntity journal, List<PostingLine> lines, MerchantStatementSnapshot statementSnapshot) {
        Map<Long, LedgerBalanceEntity> lockedBalances = lockBalances(lines.stream()
                .filter(line -> positive(line.amount()))
                .map(PostingLine::account)
                .toList());
        List<MerchantWalletStatementEntity> walletStatements = new ArrayList<>();
        int entryNo = 1;
        for (PostingLine line : lines) {
            if (!positive(line.amount())) {
                continue;
            }
            // 先锁定余额行，保证同一账户并发入账时余额前后值连续
                        LedgerBalanceEntity balance = lockedBalances.get(line.account().getId());
            BigDecimal before = scale(balance.getBalance());
            // 根据账户正常余额方向计算本次入账对余额的正负影响
                        BigDecimal change = balanceChange(line.account(), line.direction(), line.amount());
            BigDecimal after = scale(before.add(change));

            // 分录保存账户、业务单、事件、余额前后值快照，便于审计追踪
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
            MerchantWalletStatementEntity walletStatement = merchantWalletStatement(journal, entry, statementSnapshot);
            if (walletStatement != null) {
                walletStatements.add(walletStatement);
            }

            // applyEntry 同时累加借贷发生额，并在不允许负余额时阻止扣成负数
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
                throw new InsufficientLedgerBalanceException(line.account().getAccountNo());
            }
        }
        createMerchantWalletStatements(journal, walletStatements);
    }

    private void postEntriesOptimized(LedgerJournalEntity journal, List<PostingLine> lines, MerchantStatementSnapshot statementSnapshot) {
        long profileStartNanos = System.nanoTime();
        long profileLastNanos = profileStartNanos;
        StringBuilder profileSteps = new StringBuilder();
        Map<Long, LedgerBalanceEntity> lockedBalances = lockBalances(lines.stream()
                .filter(line -> positive(line.amount()))
                .map(PostingLine::account)
                .toList());
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "lock_balances");

        List<PostingEntry> postingEntries = buildPostingEntries(journal, lines, lockedBalances);
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "build_entries");
        if (postingEntries.isEmpty()) {
            logLedgerPostEntriesProfileIfSlow(journal, profileStartNanos, profileSteps);
            return;
        }

        List<LedgerEntryEntity> entries = postingEntries.stream().map(PostingEntry::entry).toList();
        int inserted = ledgerEntryDao.insertBatch(entries);
        if (inserted != entries.size()) {
            throw new IllegalStateException("Insert ledger entries failed: " + journal.getJournalNo());
        }
        for (LedgerEntryEntity entry : entries) {
            if (entry.getId() == null) {
                fillGeneratedEntryIds(journal, entries);
                break;
            }
        }
        for (LedgerEntryEntity entry : entries) {
            if (entry.getId() == null) {
                throw new IllegalStateException("Ledger entry generated id is missing: " + journal.getJournalNo() + "/" + entry.getEntryNo());
            }
        }
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "insert_entries");

        for (PostingEntry postingEntry : postingEntries) {
            PostingLine line = postingEntry.line();
            LedgerEntryEntity entry = postingEntry.entry();
            int updated = ledgerBalanceDao.applyEntry(
                    journal.getTenantId(),
                    line.account().getId(),
                    postingEntry.balanceChange(),
                    LedgerDirectionEnum.DEBIT.code().equals(line.direction()) ? scale(line.amount()) : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    LedgerDirectionEnum.CREDIT.code().equals(line.direction()) ? scale(line.amount()) : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    entry.getId(),
                    journal.getJournalNo(),
                    journal.getPostedAt(),
                    line.account().getAllowNegative()
            );
            if (updated != 1) {
                throw new InsufficientLedgerBalanceException(line.account().getAccountNo());
            }
        }
        profileLastNanos = markLedgerStep(profileSteps, profileLastNanos, "apply_balances");

        List<MerchantWalletStatementEntity> walletStatements = new ArrayList<>();
        for (PostingEntry postingEntry : postingEntries) {
            MerchantWalletStatementEntity walletStatement = merchantWalletStatement(journal, postingEntry.entry(), statementSnapshot);
            if (walletStatement != null) {
                walletStatements.add(walletStatement);
            }
        }
        createMerchantWalletStatements(journal, walletStatements);
        markLedgerStep(profileSteps, profileLastNanos, "wallet_statement");
        logLedgerPostEntriesProfileIfSlow(journal, profileStartNanos, profileSteps);
    }

    private List<PostingEntry> buildPostingEntries(LedgerJournalEntity journal, List<PostingLine> lines, Map<Long, LedgerBalanceEntity> lockedBalances) {
        List<PostingEntry> postingEntries = new ArrayList<>();
        Map<Long, BigDecimal> currentBalances = new LinkedHashMap<>();
        int entryNo = 1;
        for (PostingLine line : lines) {
            if (!positive(line.amount())) {
                continue;
            }
            LedgerBalanceEntity balance = lockedBalances.get(line.account().getId());
            BigDecimal before = currentBalances.computeIfAbsent(line.account().getId(), ignored -> scale(balance.getBalance()));
            BigDecimal change = balanceChange(line.account(), line.direction(), line.amount());
            BigDecimal after = scale(before.add(change));
            currentBalances.put(line.account().getId(), after);

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
            Instant now = Instant.now();
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            postingEntries.add(new PostingEntry(line, entry, change));
        }
        return postingEntries;
    }

    private void fillGeneratedEntryIds(LedgerJournalEntity journal, List<LedgerEntryEntity> entries) {
        Map<Integer, LedgerEntryEntity> insertedEntryMap = new LinkedHashMap<>();
        List<LedgerEntryEntity> insertedEntries = ledgerEntryDao.selectList(new QueryWrapper<LedgerEntryEntity>()
                .eq("tenant_id", journal.getTenantId())
                .eq("journal_id", journal.getId())
                .orderByAsc("entry_no"));
        for (LedgerEntryEntity insertedEntry : insertedEntries) {
            insertedEntryMap.put(insertedEntry.getEntryNo(), insertedEntry);
        }
        for (LedgerEntryEntity entry : entries) {
            LedgerEntryEntity insertedEntry = insertedEntryMap.get(entry.getEntryNo());
            if (insertedEntry != null) {
                entry.setId(insertedEntry.getId());
            }
        }
    }

    private MerchantWalletStatementEntity merchantWalletStatement(LedgerJournalEntity journal, LedgerEntryEntity entry, MerchantStatementSnapshot snapshot) {
        if (snapshot == null || entry == null || !isMerchantVisibleEntry(entry)) {
            return null;
        }
        MerchantWalletStatementEntity statement = new MerchantWalletStatementEntity();
        statement.setTenantId(entry.getTenantId());
        statement.setStatementNo(BizKeyUtils.genMerchantWalletStatementNo());
        statement.setMerchantId(entry.getOwnerId());
        statement.setMerchantNo(snapshot.merchantNo());
        statement.setMerchantAppId(snapshot.merchantAppId());
        statement.setJournalId(journal.getId());
        statement.setJournalNo(journal.getJournalNo());
        statement.setEntryId(entry.getId());
        statement.setBizType(entry.getBizType());
        statement.setBizId(entry.getBizId());
        statement.setBizNo(entry.getBizNo());
        statement.setMerchantOrderNo(snapshot.merchantOrderNo());
        statement.setEventType(entry.getEventType());
        statement.setAccountId(entry.getAccountId());
        statement.setAccountNo(entry.getAccountNo());
        statement.setAccountType(entry.getAccountType());
        statement.setCurrency(entry.getCurrency());
        statement.setEffectType(effectType(entry));
        statement.setBizAmount(scale(snapshot.bizAmount()));
        statement.setFeeAmount(scale(snapshot.feeAmount()));
        statement.setNetAmount(scale(snapshot.netAmount()));
        statement.setBalanceChange(scale(entry.getBalanceChange()));
        statement.setBalanceBefore(scale(entry.getBalanceBefore()));
        statement.setBalanceAfter(scale(entry.getBalanceAfter()));
        statement.setSourceType(journal.getSourceType());
        statement.setStatus(journal.getStatus());
        statement.setPostedAt(journal.getPostedAt());
        statement.setTraceId(journal.getTraceId());
        statement.setSummary(entry.getSummary());
        statement.setRemark(journal.getRemark());
        return statement;
    }

    private void createMerchantWalletStatements(LedgerJournalEntity journal, List<MerchantWalletStatementEntity> statements) {
        if (statements.isEmpty()) {
            return;
        }
        List<MerchantWalletStatementEntity> copy = List.copyOf(statements);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    createMerchantWalletStatementsAsync(journal.getJournalNo(), copy);
                }
            });
            return;
        }
        createMerchantWalletStatementsAsync(journal.getJournalNo(), copy);
    }

    private void createMerchantWalletStatementsAsync(String journalNo, List<MerchantWalletStatementEntity> statements) {
        AsynUtils.execute("Create merchant wallet statements", () -> insertMerchantWalletStatements(journalNo, statements));
    }

    private void insertMerchantWalletStatements(String journalNo, List<MerchantWalletStatementEntity> statements) {
        try {
            merchantWalletStatementDao.insert(statements);
        } catch (Exception ex) {
            log.warn("Create merchant wallet statements failed, journalNo={}, count={}, err={}",
                    journalNo, statements.size(), ex.getMessage());
        }
    }

    private boolean isMerchantVisibleEntry(LedgerEntryEntity entry) {
        return LedgerOwnerTypeEnum.MERCHANT.code().equals(entry.getOwnerType())
                && LedgerAccountTypeEnum.merchantVisibleTypes().contains(entry.getAccountType());
    }

    private String effectType(LedgerEntryEntity entry) {
        if (LedgerPostingEventEnum.PAYOUT_FREEZE.code().equals(entry.getEventType())) {
            return MerchantWalletStatementEffectEnum.FREEZE.code();
        }
        if (LedgerPostingEventEnum.PAYOUT_FAILED.code().equals(entry.getEventType())) {
            return MerchantWalletStatementEffectEnum.UNFREEZE.code();
        }
        if (entry.getEventType() != null && entry.getEventType().startsWith("MANUAL_")) {
            return MerchantWalletStatementEffectEnum.ADJUST.code();
        }
        return scale(entry.getBalanceChange()).compareTo(BigDecimal.ZERO) >= 0
                ? MerchantWalletStatementEffectEnum.IN.code()
                : MerchantWalletStatementEffectEnum.OUT.code();
    }

    /**
     * 锁定账户余额行     *
     * <p>如果账户已经存在但余额行缺失，会先幂等补建余额行，再使用 for update 加锁/p>
     */
    private LedgerBalanceEntity lockBalance(LedgerAccountEntity account) {
        LedgerBalanceEntity balance = selectLockedBalance(account);
        if (balance != null) {
            return balance;
        }
        initializeBalance(account);
        balance = selectLockedBalance(account);
        if (balance == null) {
            throw new IllegalStateException("Ledger balance is not configured: " + account.getAccountNo());
        }
        return balance;
    }

    private Map<Long, LedgerBalanceEntity> lockBalances(Collection<LedgerAccountEntity> accounts) {
        Map<Long, LedgerAccountEntity> accountMap = new LinkedHashMap<>();
        for (LedgerAccountEntity account : accounts) {
            if (account != null && account.getId() != null) {
                accountMap.putIfAbsent(account.getId(), account);
            }
        }
        if (accountMap.isEmpty()) {
            return Map.of();
        }

        Long tenantId = accountMap.values().iterator().next().getTenantId();
        List<LedgerBalanceEntity> balances = ledgerBalanceDao.selectList(new QueryWrapper<LedgerBalanceEntity>()
                .eq("tenant_id", tenantId)
                .in("account_id", accountMap.keySet())
                .orderByAsc("account_id")
                .last("for update"));
        Map<Long, LedgerBalanceEntity> result = new LinkedHashMap<>();
        for (LedgerBalanceEntity balance : balances) {
            result.put(balance.getAccountId(), balance);
        }
        if (result.size() == accountMap.size()) {
            return result;
        }

        for (Map.Entry<Long, LedgerAccountEntity> entry : accountMap.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), lockBalance(entry.getValue()));
            }
        }
        return result;
    }

    private LedgerBalanceEntity selectLockedBalance(LedgerAccountEntity account) {
        return ledgerBalanceDao.selectOne(new QueryWrapper<LedgerBalanceEntity>()
                .eq("tenant_id", account.getTenantId())
                .eq("account_id", account.getId())
                .last("limit 1 for update"));
    }

    /**
     * 确保账户存在对应余额行     *
     * <p>主要用于初始化漏补或并发创建场景；重复插入由唯一键和 DuplicateKeyException 兜住/p>
     */
    private void initializeBalance(LedgerAccountEntity account) {
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

    /**
     * 按资金主体、账户类型和币种获取账户     *
     * <p>商户账户允许自动创建；系统、平台等公共账户要求提前配置/p>
     */
    private LedgerAccountEntity merchantAccountForPosting(Long tenantId, Long merchantId, String accountType, String currency) {
        String normalizedCurrency = normalize(currency);
        MerchantAccountCacheKey cacheKey = new MerchantAccountCacheKey(tenantId, merchantId, accountType, normalizedCurrency);
        LedgerAccountEntity cached = getCachedMerchantAccount(cacheKey);
        if (cached != null) {
            return cached;
        }
        LedgerAccountEntity account = ledgerAccountDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", SubjectTypeEnum.MERCHANT.code())
                .eq("owner_id", merchantId)
                .eq("account_type", accountType)
                .eq("currency", normalizedCurrency)
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
        if (account != null) {
            cacheMerchantAccount(cacheKey, account);
            return account;
        }
        account = ledgerAccountService.requireMerchantAccount(tenantId, merchantId, accountType, normalizedCurrency);
        cacheMerchantAccount(cacheKey, account);
        return account;
    }

    private LedgerAccountEntity getCachedMerchantAccount(MerchantAccountCacheKey cacheKey) {
        CachedLedgerAccount cached = merchantAccountCache.get(cacheKey);
        if (cached == null) {
            return null;
        }
        if (cached.expireAtMillis() <= System.currentTimeMillis()) {
            merchantAccountCache.remove(cacheKey, cached);
            return null;
        }
        return cached.account();
    }

    private void cacheMerchantAccount(MerchantAccountCacheKey cacheKey, LedgerAccountEntity account) {
        if (account == null || account.getId() == null || !StatusEnum.NORMAL.code().equals(account.getStatus())) {
            return;
        }
        if (merchantAccountCache.size() >= MERCHANT_ACCOUNT_CACHE_MAX_SIZE) {
            pruneMerchantAccountCache();
        }
        merchantAccountCache.put(cacheKey, new CachedLedgerAccount(account, System.currentTimeMillis() + MERCHANT_ACCOUNT_CACHE_TTL_MILLIS));
    }

    private void pruneMerchantAccountCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<MerchantAccountCacheKey, CachedLedgerAccount>> iterator = merchantAccountCache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expireAtMillis() <= now) {
                iterator.remove();
            }
        }
        if (merchantAccountCache.size() < MERCHANT_ACCOUNT_CACHE_MAX_SIZE) {
            return;
        }
        int removeCount = Math.max(1, MERCHANT_ACCOUNT_CACHE_MAX_SIZE / 10);
        iterator = merchantAccountCache.entrySet().iterator();
        while (iterator.hasNext() && removeCount-- > 0) {
            iterator.next();
            iterator.remove();
        }
    }

    private LedgerAccountEntity account(Long tenantId, String ownerType, Long ownerId, String accountType, String currency) {
        String normalizedCurrency = normalize(currency);
        if (SubjectTypeEnum.MERCHANT.code().equals(ownerType)
                && (LedgerAccountTypeEnum.AVAILABLE.matches(accountType)
                || LedgerAccountTypeEnum.FROZEN.matches(accountType)
                || LedgerAccountTypeEnum.PENDING_SETTLE.matches(accountType))) {
            // 商户开户存在补偿能力，缺失时自动创建对应账户和余额
                        return ledgerAccountService.requireMerchantAccount(tenantId, ownerId, accountType, currency);
        }
        LedgerAccountEntity account = ledgerAccountDao.selectOne(new QueryWrapper<LedgerAccountEntity>()
                .eq("tenant_id", tenantId)
                .eq("owner_type", ownerType)
                .eq("owner_id", ownerId)
                .eq("account_type", accountType)
                .eq("currency", normalizedCurrency)
                .eq("status", StatusEnum.NORMAL.code())
                .last("limit 1"));
        if (account == null) {
            throw new GkException("Ledger account is not configured: " + ownerType + "/" + accountType + "/" + currency);
        }
        return account;
    }

    /**
     * 获取清算账户     *
     * <p>订单携带 pspAccountId 时使PSP 清算账户；没有时使用内部清算账户/p>
     */
    private LedgerAccountEntity clearingAccount(Long tenantId, Long pspAccountId, String currency) {
        if (pspAccountId != null) {
            return ledgerAccountService.requirePspAccount(tenantId, pspAccountId, LedgerAccountTypeEnum.CLEARING.code(), currency);
        }
        return account(tenantId, LedgerOwnerTypeEnum.INTERNAL.code(), 0L, LedgerAccountTypeEnum.CLEARING.code(), currency);
    }

    /**
     * 根据账户 ID 获取账户     *
     * <p>冻结消费和释放使hold 中保存的账户 ID，避免账户类型变化影响历史冻结记录/p>
     */
    private LedgerAccountEntity account(Long accountId) {
        LedgerAccountEntity account = ledgerAccountDao.selectById(accountId);
        if (account == null || !StatusEnum.NORMAL.code().equals(account.getStatus())) {
            throw new IllegalStateException("Ledger account is not available: " + accountId);
        }
        return account;
    }

    /**
     * 查询指定业务事件是否已经生成凭证     */
    private LedgerJournalEntity findJournal(Long tenantId, String bizType, String bizNo, String eventType) {
        return ledgerJournalDao.selectOne(new QueryWrapper<LedgerJournalEntity>()
                .eq("tenant_id", tenantId)
                .eq("biz_type", bizType)
                .eq("biz_no", bizNo)
                .eq("event_type", eventType)
                .last("limit 1"));
    }

    /**
     * 查询代付订单的冻结记录     */
    private LedgerHoldEntity findHold(Long tenantId, String bizNo) {
        return ledgerHoldDao.selectOne(new QueryWrapper<LedgerHoldEntity>()
                .eq("tenant_id", tenantId)
                .eq("biz_type", BizTypeEnum.PAYOUT_ORDER.code())
                .eq("biz_no", bizNo)
                .last("limit 1"));
    }

    /**
     * 获取并锁定仍在冻结中的代hold     *
     * <p>代付成功消费和失败释放都必须HOLDING 状态推进，防止已消已释放的金额再次处理/p>
     */
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

    /**
     * 根据账户正常方向计算余额变化     *
     * <p>例如 CREDIT 正常账户记贷方为正，记借方为负；DEBIT 正常账户则相反/p>
     */
    private BigDecimal balanceChange(LedgerAccountEntity account, String direction, BigDecimal amount) {
        BigDecimal scaled = scale(amount);
        return StringUtils.equalsIgnoreCase(account.getNormalSide(), direction) ? scaled : scaled.negate();
    }

    /**
     * 构造稳定幂等键     */
    private String idempotencyKey(String bizNo, String eventType) {
        return bizNo + ":" + eventType;
    }

    /**
     * 计算代付需要从商户可用余额扣出的总金额     */
    private BigDecimal payoutTotalDebit(PayoutPostingRequest request) {
        return amountOrDefault(request.getTotalDebitAmount(), request.getAmount().add(defaultZero(request.getMerchantFeeAmount())));
    }

    /**
     * 校验金额不能为负数     */
    private void requireNonNegative(BigDecimal value, String field) {
        if (scale(value).compareTo(BigDecimal.ZERO) < 0) {
            throw new GkException("Invalid posting amount: " + field);
        }
    }

    /**
     * 校验代收成功/结算释放请求的必要字段     */
    private void validatePaySuccess(PaySuccessPostingRequest request) {
        if (request == null || request.getTenantId() == null || request.getMerchantId() == null || StringUtils.isBlank(request.getPayOrderNo())
                || StringUtils.isBlank(request.getCurrency()) || !positive(request.getAmount())) {
            throw new GkException("Invalid pay success posting request");
        }
    }

    /**
     * 校验代付冻结/成功/失败释放请求的必要字段     */
    private void validatePayout(PayoutPostingRequest request) {
        if (request == null || request.getTenantId() == null || request.getMerchantId() == null || StringUtils.isBlank(request.getPayoutOrderNo())
                || StringUtils.isBlank(request.getCurrency()) || !positive(request.getAmount())) {
            throw new GkException("Invalid payout posting request");
        }
        requireNonNegative(defaultZero(request.getMerchantFeeAmount()), "merchantFeeAmount");
        if (request.getTotalDebitAmount() != null && !positive(request.getTotalDebitAmount())) {
            throw new GkException("Invalid payout posting request: totalDebitAmount");
        }
    }

    /**
     * 校验人工调账请求并解析调账类型     */
    private MerchantBalanceAdjustTypeEnum validateMerchantBalanceAdjust(MerchantBalanceAdjustPostingRequest request) {
        if (request == null || request.getTenantId() == null || request.getMerchantId() == null
                || StringUtils.isBlank(request.getAdjustOrderNo()) || StringUtils.isBlank(request.getCurrency())
                || !positive(request.getAmount())) {
            throw new GkException("Invalid merchant balance adjust posting request");
        }
        MerchantBalanceAdjustTypeEnum adjustType = StringCodeEnum.fromCode(MerchantBalanceAdjustTypeEnum.class, request.getAdjustType());
        if (adjustType == null) {
            throw new GkException("Invalid merchant balance adjust type");
        }
        return adjustType;
    }

    /**
     * 判断金额是否大于 0     */
    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 金额为空时使用兜底值，并统一小数精度     */
    private BigDecimal amountOrDefault(BigDecimal value, BigDecimal fallback) {
        return scale(value == null ? fallback : value);
    }

    /**
     * 金额为空时按 0 处理，并统一小数精度     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : scale(value);
    }

    /**
     * 统一金额精度，避免入账时出现小数位不一致     */
    private BigDecimal scale(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 规范化字符串编码值，目前主要用于币种转大写     */
    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }

    private long markLedgerStep(StringBuilder steps, long lastNanos, String step) {
        long now = System.nanoTime();
        if (!steps.isEmpty()) {
            steps.append(", ");
        }
        steps.append(step).append('=').append(toMillis(now - lastNanos)).append("ms");
        return now;
    }

    private void logLedgerProfileIfSlow(String action,
                                        PayoutPostingRequest request,
                                        long startNanos,
                                        StringBuilder steps,
                                        String result) {
        long totalMs = toMillis(System.nanoTime() - startNanos);
        if (totalMs < SLOW_LEDGER_PROFILE_MILLIS) {
            return;
        }
        log.info("Ledger posting profile action={}, result={}, tenantId={}, merchantId={}, orderNo={}, total={}ms, steps=[{}]",
                action,
                result,
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getMerchantId(),
                request == null ? null : request.getPayoutOrderNo(),
                totalMs,
                steps);
    }

    private void logLedgerPostEntriesProfileIfSlow(LedgerJournalEntity journal, long startNanos, StringBuilder steps) {
        long totalMs = toMillis(System.nanoTime() - startNanos);
        if (totalMs < SLOW_LEDGER_PROFILE_MILLIS) {
            return;
        }
        log.info("Ledger post entries profile tenantId={}, journalNo={}, bizType={}, bizNo={}, eventType={}, total={}ms, steps=[{}]",
                journal == null ? null : journal.getTenantId(),
                journal == null ? null : journal.getJournalNo(),
                journal == null ? null : journal.getBizType(),
                journal == null ? null : journal.getBizNo(),
                journal == null ? null : journal.getEventType(),
                totalMs,
                steps);
    }

    private long toMillis(long nanos) {
        return Math.max(0L, nanos / 1_000_000L);
    }

    /**
     * 待入账分录的内存模型     *
     * @param account 入账账户
     * @param direction 借贷方向
     * @param amount 入账金额，必须为正数
     * @param summary 分录摘要
     */
    private record PostingLine(LedgerAccountEntity account, String direction, BigDecimal amount, String summary) {
    }

    private record PostingEntry(PostingLine line, LedgerEntryEntity entry, BigDecimal balanceChange) {
    }

    private record MerchantAccountCacheKey(Long tenantId, Long merchantId, String accountType, String currency) {
    }

    private record CachedLedgerAccount(LedgerAccountEntity account, long expireAtMillis) {
    }

    private record MerchantStatementSnapshot(String merchantNo,
                                             Long merchantAppId,
                                             String merchantOrderNo,
                                             BigDecimal bizAmount,
                                             BigDecimal feeAmount,
                                             BigDecimal netAmount) {
        static MerchantStatementSnapshot pay(PaySuccessPostingRequest request, BigDecimal netAmount, BigDecimal feeAmount) {
            return new MerchantStatementSnapshot(
                    request.getMerchantNo(),
                    request.getMerchantAppId(),
                    request.getMerchantOrderNo(),
                    request.getAmount(),
                    feeAmount,
                    netAmount
            );
        }

        static MerchantStatementSnapshot payout(PayoutPostingRequest request, BigDecimal netAmount) {
            return new MerchantStatementSnapshot(
                    request.getMerchantNo(),
                    request.getMerchantAppId(),
                    request.getMerchantOrderNo(),
                    request.getAmount(),
                    request.getMerchantFeeAmount(),
                    netAmount
            );
        }

        static MerchantStatementSnapshot adjust(MerchantBalanceAdjustPostingRequest request, BigDecimal netAmount) {
            return new MerchantStatementSnapshot(
                    request.getMerchantNo(),
                    request.getMerchantAppId(),
                    request.getMerchantOrderNo(),
                    request.getAmount(),
                    BigDecimal.ZERO,
                    netAmount
            );
        }
    }
}
