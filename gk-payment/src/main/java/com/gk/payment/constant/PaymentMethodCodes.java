package com.gk.payment.constant;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

/**
 * 系统标准支付方式编码 */
public final class PaymentMethodCodes {
    public static final String BANK_CARD = "BANK_CARD";

    private PaymentMethodCodes() {
    }

    /**
     * 判断是否为银行卡转账方式     */
    public static boolean isBankCard(String methodCode) {
        return Strings.CI.equals(StringUtils.trim(methodCode), BANK_CARD);
    }
}
