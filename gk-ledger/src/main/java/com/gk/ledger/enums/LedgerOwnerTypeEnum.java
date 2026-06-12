package com.gk.ledger.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 账本资金主体类型。
 */
public enum LedgerOwnerTypeEnum implements StringCodeEnum {
    PLATFORM("PLATFORM", "平台", "enum.ledgerOwnerType.platform"),
    TENANT("TENANT", "租户", "enum.ledgerOwnerType.tenant"),
    MERCHANT("MERCHANT", "商户", "enum.ledgerOwnerType.merchant"),
    PSP("PSP", "通道", "enum.ledgerOwnerType.psp"),
    SYSTEM("SYSTEM", "系统", "enum.ledgerOwnerType.system");

    private final String code;
    private final String label;
    private final String i18nKey;

    LedgerOwnerTypeEnum(String code, String label, String i18nKey) {
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
