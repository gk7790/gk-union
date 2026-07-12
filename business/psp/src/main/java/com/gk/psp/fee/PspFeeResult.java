package com.gk.psp.fee;

import com.gk.psp.entity.PspFeeRuleEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PspFeeResult {
    private PspFeeRuleEntity rule;
    private BigDecimal pspFeeAmount;
    private String snapshotJson;
}
