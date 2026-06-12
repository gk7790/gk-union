package com.gk.payment.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * 代收订单结算状态。
 */
public enum SettleStatusEnum implements StringCodeEnum {
    PENDING("PENDING", "待结算", "enum.settleStatus.pending"),
    RELEASED("RELEASED", "已释放", "enum.settleStatus.released"),
    HELD("HELD", "已冻结", "enum.settleStatus.held"),
    CANCELLED("CANCELLED", "已取消", "enum.settleStatus.cancelled");

    private final String code;
    private final String label;
    private final String i18nKey;

    SettleStatusEnum(String code, String label, String i18nKey) {
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
