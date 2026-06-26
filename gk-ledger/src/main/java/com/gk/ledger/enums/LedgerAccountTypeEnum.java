package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

import java.util.Set;

/**
 * 账本账户类型
 */
@EnumDict("ledgerAccountType")
public enum LedgerAccountTypeEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    AVAILABLE("AVAILABLE", "可用", "enum.ledgerAccountType.available"),
    @Style(StyleType.WARNING)
    PENDING_SETTLE("PENDING_SETTLE", "待结", "enum.ledgerAccountType.pendingSettle"),
    @Style(StyleType.DANGER)
    FROZEN("FROZEN", "冻结", "enum.ledgerAccountType.frozen"),
    @Style(StyleType.PRIMARY)
    CLEARING("CLEARING", "清算", "enum.ledgerAccountType.clearing"),
    @Style(StyleType.PRIMARY)
    FEE_INCOME("FEE_INCOME", "手续费收", "enum.ledgerAccountType.feeIncome");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerAccountTypeEnum(String code, String label, String i18nKey) {
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

    /**
     * 商户可见账户类型
     */
    public static Set<String> merchantVisibleTypes() {
        return Set.of(
                AVAILABLE.code(),
                PENDING_SETTLE.code(),
                FROZEN.code()
        );
    }
}
