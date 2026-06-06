package com.gk.ledger.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gk.ledger.account.dao.LedgerAccountDao;
import com.gk.ledger.account.dao.LedgerEntryDao;
import com.gk.ledger.account.dao.LedgerJournalDao;
import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.account.entity.LedgerAccountEntity;
import com.gk.ledger.account.entity.LedgerEntryEntity;
import com.gk.ledger.account.entity.LedgerJournalEntity;
import com.gk.ledger.account.service.LedgerPostingService;
import com.gk.ledger.account.service.LedgerPostingValidator;
import com.gk.ledger.common.enums.BalanceSideEnum;
import com.gk.ledger.common.enums.LedgerAccountTypeEnum;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import com.gk.ledger.common.enums.LedgerJournalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LedgerPostingServiceImpl implements LedgerPostingService {
    private final LedgerPostingValidator validator;
    private final LedgerJournalDao ledgerJournalDao;
    private final LedgerEntryDao ledgerEntryDao;
    private final LedgerAccountDao ledgerAccountDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String post(LedgerPostingCommand command) {
        validator.validate(command);

        Long existing = ledgerJournalDao.selectCount(new QueryWrapper<LedgerJournalEntity>()
                .eq("biz_type", command.bizType())
                .eq("biz_no", command.bizNo()));
        if (existing != null && existing > 0) {
            LedgerJournalEntity posted = ledgerJournalDao.selectOne(new QueryWrapper<LedgerJournalEntity>()
                    .eq("biz_type", command.bizType())
                    .eq("biz_no", command.bizNo())
                    .last("LIMIT 1"));
            return posted.getJournalNo();
        }

        LedgerJournalEntity journal = new LedgerJournalEntity();
        journal.setTenantId(command.tenantId());
        journal.setMerchantId(command.merchantId());
        journal.setJournalNo(command.journalNo());
        journal.setBizType(command.bizType());
        journal.setBizNo(command.bizNo());
        journal.setCurrency(command.currency());
        journal.setTotalAmount(command.totalAmount());
        journal.setStatus(LedgerJournalStatusEnum.POSTED.name());
        journal.setPostedAt(Instant.now());
        journal.setRemark(command.summary());
        ledgerJournalDao.insert(journal);

        for (LedgerEntryLine line : command.entries()) {
            LedgerAccountEntity account = ledgerAccountDao.selectByIdForUpdate(line.accountId());
            if (account == null) {
                throw new IllegalArgumentException("Ledger account does not exist: " + line.accountId());
            }
            if (!command.currency().equals(account.getCurrency())) {
                throw new IllegalArgumentException("Ledger account currency does not match posting currency");
            }

            BigDecimal nextBalance = calculateNextBalance(account, line);
            if (nextBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Ledger account balance cannot be negative: " + account.getAccountNo());
            }

            LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(line.direction());
            if (direction == LedgerDirectionEnum.DEBIT) {
                account.setDebitTotal(account.getDebitTotal().add(line.amount()));
            } else {
                account.setCreditTotal(account.getCreditTotal().add(line.amount()));
            }
            account.setBalance(nextBalance);
            ledgerAccountDao.updateById(account);

            LedgerEntryEntity entry = new LedgerEntryEntity();
            entry.setTenantId(command.tenantId());
            entry.setMerchantId(command.merchantId());
            entry.setJournalId(journal.getId());
            entry.setJournalNo(command.journalNo());
            entry.setAccountId(account.getId());
            entry.setAccountNo(account.getAccountNo());
            entry.setDirection(direction.name());
            entry.setAmount(line.amount());
            entry.setBalanceAfter(nextBalance);
            entry.setSummary(line.summary());
            ledgerEntryDao.insert(entry);
        }

        return journal.getJournalNo();
    }

    private BigDecimal calculateNextBalance(LedgerAccountEntity account, LedgerEntryLine line) {
        LedgerAccountTypeEnum accountType = LedgerAccountTypeEnum.fromCode(account.getAccountType());
        LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(line.direction());
        boolean increases = (accountType.balanceSide() == BalanceSideEnum.DEBIT && direction == LedgerDirectionEnum.DEBIT)
                || (accountType.balanceSide() == BalanceSideEnum.CREDIT && direction == LedgerDirectionEnum.CREDIT);
        if (increases) {
            return account.getBalance().add(line.amount());
        }
        return account.getBalance().subtract(line.amount());
    }
}
