package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerEntryLine;
import com.gk.ledger.account.dto.LedgerPostingCommand;
import com.gk.ledger.common.enums.LedgerDirectionEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerPostingValidatorTest {

    private final LedgerPostingValidator validator = new LedgerPostingValidator();

    @Test
    void acceptsBalancedPosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1001",
                "PAY_SUCCESS",
                "P1001",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "platform in transit"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("98.00"), "merchant frozen"),
                        new LedgerEntryLine(3L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("2.00"), "platform revenue")
                )
        );

        assertDoesNotThrow(() -> validator.validate(command));
    }

    @Test
    void rejectsUnbalancedPosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1002",
                "PAY_SUCCESS",
                "P1002",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "platform in transit"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), new BigDecimal("97.00"), "merchant frozen")
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }

    @Test
    void rejectsNonPositiveAmount() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1003",
                "PAY_SUCCESS",
                "P1003",
                "PHP",
                new BigDecimal("0.00"),
                "pay success",
                List.of(
                        new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), BigDecimal.ZERO, "zero"),
                        new LedgerEntryLine(2L, LedgerDirectionEnum.CREDIT.name(), BigDecimal.ZERO, "zero")
                )
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }

    @Test
    void rejectsSingleLinePosting() {
        LedgerPostingCommand command = new LedgerPostingCommand(
                1L,
                10L,
                "J1004",
                "PAY_SUCCESS",
                "P1004",
                "PHP",
                new BigDecimal("100.00"),
                "pay success",
                List.of(new LedgerEntryLine(1L, LedgerDirectionEnum.DEBIT.name(), new BigDecimal("100.00"), "one line"))
        );

        assertThrows(IllegalArgumentException.class, () -> validator.validate(command));
    }
}
