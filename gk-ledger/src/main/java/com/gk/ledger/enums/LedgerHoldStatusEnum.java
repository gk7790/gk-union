package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * 冻结单状态。
 */
@EnumDict("ledgerHoldStatus")
public enum LedgerHoldStatusEnum implements StringCodeEnum {
    HOLDING("HOLDING", "冻结中", "enum.ledgerHoldStatus.holding"),
    PART_RELEASED("PART_RELEASED", "部分解冻", "enum.ledgerHoldStatus.partReleased"),
    EXPIRED("EXPIRED", "已过期待处理", "enum.ledgerHoldStatus.expired"),
    RELEASED("RELEASED", "已解冻", "enum.ledgerHoldStatus.released"),
    CONSUMED("CONSUMED", "已消费", "enum.ledgerHoldStatus.consumed"),
    CANCELLED("CANCELLED", "已取消", "enum.ledgerHoldStatus.cancelled");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerHoldStatusEnum(String code, String label, String i18nKey) {
        this.code = code;
        this.label = label;
        this.i18nKey = i18nKey;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String i18nKey() {
        return i18nKey;
    }
}
