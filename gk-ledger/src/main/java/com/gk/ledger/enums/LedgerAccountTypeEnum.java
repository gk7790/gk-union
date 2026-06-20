package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

import java.util.Set;

/**
 * 账本账户类型。
 */
@EnumDict("ledgerAccountType")
public enum LedgerAccountTypeEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    MERCHANT_AVAILABLE("MERCHANT_AVAILABLE", "商户可用", "enum.ledgerAccountType.merchantAvailable"),
    @Style(StyleType.WARNING)
    MERCHANT_PENDING_SETTLE("MERCHANT_PENDING_SETTLE", "商户待结算", "enum.ledgerAccountType.merchantPendingSettle"),
    @Style(StyleType.DANGER)
    MERCHANT_FROZEN("MERCHANT_FROZEN", "商户冻结", "enum.ledgerAccountType.merchantFrozen"),
    @Style(StyleType.PRIMARY)
    INTERNAL_CLEARING("INTERNAL_CLEARING", "内部清算", "enum.ledgerAccountType.internalClearing"),
    @Style(StyleType.PRIMARY)
    PSP_CLEARING("PSP_CLEARING", "PSP清算", "enum.ledgerAccountType.pspClearing"),
    @Style(StyleType.SUCCESS)
    INTERNAL_FEE_INCOME("INTERNAL_FEE_INCOME", "内部手续费收入", "enum.ledgerAccountType.internalFeeIncome");

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
                MERCHANT_AVAILABLE.code(),
                MERCHANT_PENDING_SETTLE.code(),
                MERCHANT_FROZEN.code()
        );
    }
}
