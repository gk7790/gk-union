package com.gk.ledger.posting;

import lombok.Data;

@Data
public class LedgerPostingResult {
    private boolean posted;
    private String journalNo;
    private String holdNo;

    public static LedgerPostingResult posted(String journalNo) {
        LedgerPostingResult result = new LedgerPostingResult();
        result.setPosted(true);
        result.setJournalNo(journalNo);
        return result;
    }

    public static LedgerPostingResult posted(String journalNo, String holdNo) {
        LedgerPostingResult result = posted(journalNo);
        result.setHoldNo(holdNo);
        return result;
    }

    public static LedgerPostingResult existed(String journalNo, String holdNo) {
        LedgerPostingResult result = new LedgerPostingResult();
        result.setPosted(false);
        result.setJournalNo(journalNo);
        result.setHoldNo(holdNo);
        return result;
    }
}
