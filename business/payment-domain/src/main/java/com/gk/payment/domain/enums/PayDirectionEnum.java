package com.gk.payment.domain.enums;

import com.gk.common.enums.StringCodeEnum;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StyleType;

/**
 * 支付方向：代收 / 代付。
 */
@EnumDict("payDirection")
public enum PayDirectionEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    PAYIN("PAYIN", "代收", "enum.payDirection.payin"),

    @Style(StyleType.WARNING)
    PAYOUT("PAYOUT", "代付", "enum.payDirection.payout");

    private final String code;
    private final String label;
    private final String i18nKey;

    PayDirectionEnum(String code, String label, String i18nKey) {
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
