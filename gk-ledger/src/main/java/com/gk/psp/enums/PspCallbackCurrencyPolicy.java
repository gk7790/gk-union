package com.gk.psp.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * PSP 回调币种解析策略。
 */
public enum PspCallbackCurrencyPolicy implements StringCodeEnum {
    /**
     * PSP 未回传 currency 时，回退使用平台订单币种（单币种通道场景）。
     */
    ORDER_FALLBACK("ORDER_FALLBACK", "回退订单币种"),

    /**
     * PSP 必须回传 currency，缺失则拒收。
     */
    REQUIRE_PSP("REQUIRE_PSP", "要求PSP回传币种");

    private final String code;
    private final String label;

    PspCallbackCurrencyPolicy(String code, String label) {
        this.code = code;
        this.label = label;
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
        return null;
    }
}
