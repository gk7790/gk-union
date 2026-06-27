package com.gk.ledger.exception;

public class InsufficientLedgerBalanceException extends RuntimeException {
    public InsufficientLedgerBalanceException(String accountNo) {
        super("Insufficient ledger balance: " + accountNo);
    }
}
