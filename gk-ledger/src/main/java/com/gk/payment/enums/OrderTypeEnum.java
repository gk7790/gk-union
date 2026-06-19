package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 订单状态日志中的订单类型（代收/代付）。
 */
@EnumDict("orderType")
public enum OrderTypeEnum implements StringCodeEnum {
    @Style(StyleType.SUCCESS)
    PAY("PAY", "代收", "enum.orderType.pay"),

    @Style(StyleType.WARNING)
    PAYOUT("PAYOUT", "代付", "enum.orderType.payout");

    private final String code;
    private final String label;
    private final String i18nKey;

    OrderTypeEnum(String code, String label, String i18nKey) {
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
