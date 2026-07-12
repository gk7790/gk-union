package com.gk.psp.adapter;

import com.gk.psp.balance.PspBalanceSnap;
import com.gk.psp.request.PspBalanceRequest;

public interface PspBalanceAdapter {
    boolean supports(String pspCode);

    PspBalanceSnap queryBalance(PspBalanceRequest request);
}
