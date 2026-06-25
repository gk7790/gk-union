package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 冻结单状态�? */
@EnumDict("ledgerHoldStatus")
public enum LedgerHoldStatusEnum implements StringCodeEnum {
    @Style(StyleType.WARNING)
    HOLDING("HOLDING", "冻结�?, "enum.ledgerHoldStatus.holding"),

    @Style(StyleType.INFO)
    PART_RELEASED("PART_RELEASED", "部分解冻", "enum.ledgerHoldStatus.partReleased"),

    @Style(StyleType.DANGER)
    EXPIRED("EXPIRED", "已过期待处理", "enum.ledgerHoldStatus.expired"),

    @Style(StyleType.SUCCESS)
    RELEASED("RELEASED", "已解�?, "enum.ledgerHoldStatus.released"),

    @Style(StyleType.SUCCESS)
    CONSUMED("CONSUMED", "已消�?, "enum.ledgerHoldStatus.consumed"),

    @Style(StyleType.DANGER)
    CANCELLED("CANCELLED", "已取�?, "enum.ledgerHoldStatus.cancelled");

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
