package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LedgerPostingValidator {

    public void validate(LedgerPostingCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Posting command is required");
        }
        if (StringUtils.isBlank(command.journalNo())) {
            throw new IllegalArgumentException("Journal number is required");
        }
        if (StringUtils.isBlank(command.bizType())) {
            throw new IllegalArgumentException("Business type is required");
        }
        if (StringUtils.isBlank(command.bizNo())) {
            throw new IllegalArgumentException("Business number is required");
        }
        if (StringUtils.isBlank(command.currency())) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (command.totalAmount() == null || command.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        List<LedgerEntryLine> entries = command.entries();
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("Posting requires at least two entry lines");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (LedgerEntryLine entry : entries) {
            if (entry.accountId() == null) {
                throw new IllegalArgumentException("Entry account id is required");
            }
            if (entry.amount() == null || entry.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Entry amount must be positive");
            }
            LedgerDirectionEnum direction = LedgerDirectionEnum.valueOf(entry.direction());
            if (direction == LedgerDirectionEnum.DEBIT) {
                debitTotal = debitTotal.add(entry.amount());
            } else {
                creditTotal = creditTotal.add(entry.amount());
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException("Debit total must equal credit total");
        }
        if (debitTotal.compareTo(command.totalAmount()) != 0) {
            throw new IllegalArgumentException("Posting total amount must equal debit total");
        }
    }
}
