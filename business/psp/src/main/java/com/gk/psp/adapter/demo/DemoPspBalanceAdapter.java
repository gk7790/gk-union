package com.gk.psp.adapter.demo;

import com.gk.psp.adapter.PspBalanceAdapter;
import com.gk.psp.balance.PspBalanceSnap;
import com.gk.psp.request.PspBalanceRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

@Component
public class DemoPspBalanceAdapter implements PspBalanceAdapter {
    @Override
    public boolean supports(String pspCode) {
        if (StringUtils.isBlank(pspCode)) {
            return false;
        }
        String normalized = pspCode.toUpperCase(Locale.ROOT);
        return "DEMO".equals(normalized) || "DEMO_PSP".equals(normalized) || "PH_DEMO_PSP".equals(normalized);
    }

    @Override
    public PspBalanceSnap queryBalance(PspBalanceRequest request) {
        PspBalanceSnap snap = new PspBalanceSnap();
        snap.setTenantId(request.getTenantId());
        snap.setPspId(request.getPspId());
        snap.setPspCode(request.getPspCode());
        snap.setPspAccountId(request.getPspAccountId());
        snap.setPspAccountNo(request.getPspAccountNo());
        snap.setCurrency("PHP");
        snap.setAvailableBalance(new BigDecimal("999999999.00"));
        snap.setFrozenBalance(BigDecimal.ZERO);
        snap.setTotalBalance(snap.getAvailableBalance());
        snap.setStatus(PspBalanceSnap.STATUS_NORMAL);
        snap.setSource(PspBalanceSnap.SOURCE_SYSTEM);
        snap.setUpdatedAt(Instant.now());
        snap.setRawResponseJson("{\"mock\":true}");
        return snap;
    }
}
