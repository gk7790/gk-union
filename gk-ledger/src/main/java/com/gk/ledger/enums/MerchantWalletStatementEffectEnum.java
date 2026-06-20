package com.gk.ledger.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

@EnumDict("merchantWalletStatementEffect")
public enum MerchantWalletStatementEffectEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    IN("IN", "收入", "enum.merchantWalletStatementEffect.in"),

    @Style(StyleType.DANGER)
    OUT("OUT", "支出", "enum.merchantWalletStatementEffect.out"),

    @Style(StyleType.WARNING)
    FREEZE("FREEZE", "冻结", "enum.merchantWalletStatementEffect.freeze"),

    @Style(StyleType.INFO)
    UNFREEZE("UNFREEZE", "解冻", "enum.merchantWalletStatementEffect.unfreeze"),

    @Style(StyleType.PRIMARY)
    ADJUST("ADJUST", "调账", "enum.merchantWalletStatementEffect.adjust");

    private final String code;
    private final String label;
    private final String i18nKey;

    MerchantWalletStatementEffectEnum(String code, String label, String i18nKey) {
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
