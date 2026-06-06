package com.gk.ledger.account.service;

import com.gk.ledger.account.dto.LedgerPostingCommand;

public interface LedgerPostingService {
    String post(LedgerPostingCommand command);
}
