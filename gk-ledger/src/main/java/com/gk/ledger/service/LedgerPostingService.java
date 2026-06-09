package com.gk.ledger.service;

import com.gk.ledger.posting.LedgerPostingResult;
import com.gk.ledger.posting.PaySuccessPostingRequest;
import com.gk.ledger.posting.PayoutPostingRequest;

public interface LedgerPostingService {
    LedgerPostingResult postPaySuccess(PaySuccessPostingRequest request);

    LedgerPostingResult freezePayout(PayoutPostingRequest request);

    LedgerPostingResult postPayoutSuccess(PayoutPostingRequest request);

    LedgerPostingResult releasePayout(PayoutPostingRequest request);
}
