package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * 代收订单状态。
 */
@EnumDict("payOrderStatus")
public enum PayOrderStatusEnum implements StringCodeEnum {
    CREATED("CREATED", "已创建", "enum.payOrderStatus.created"),
    PROCESSING("PROCESSING", "处理中", "enum.payOrderStatus.processing"),
    SUCCESS("SUCCESS", "成功", "enum.payOrderStatus.success"),
    FAILED("FAILED", "失败", "enum.payOrderStatus.failed"),
    CLOSED("CLOSED", "已关闭", "enum.payOrderStatus.closed");

    private final String code;
    private final String label;
    private final String i18nKey;

    PayOrderStatusEnum(String code, String label, String i18nKey) {
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
