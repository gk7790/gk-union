package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * 账本账户类型。
 */
@EnumDict("ledgerAccountType")
public enum LedgerAccountTypeEnum implements StringCodeEnum {
    MERCHANT_AVAILABLE("MERCHANT_AVAILABLE", "商户可用", "enum.ledgerAccountType.merchantAvailable"),
    MERCHANT_PENDING_SETTLE("MERCHANT_PENDING_SETTLE", "商户待结算", "enum.ledgerAccountType.merchantPendingSettle"),
    MERCHANT_FROZEN("MERCHANT_FROZEN", "商户冻结", "enum.ledgerAccountType.merchantFrozen"),
    SYSTEM_CLEARING("SYSTEM_CLEARING", "系统清算", "enum.ledgerAccountType.systemClearing"),
    PSP_CLEARING("PSP_CLEARING", "PSP清算", "enum.ledgerAccountType.pspClearing"),
    PLATFORM_FEE_INCOME("PLATFORM_FEE_INCOME", "平台手续费收入", "enum.ledgerAccountType.platformFeeIncome");

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
}
