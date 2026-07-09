package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 代收订单状态 */
@EnumDict("payinOrderStatus")
public enum PayinOrderStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    CREATED("CREATED", "已创建", "enum.payinOrderStatus.created"),

    @Style(StyleType.PRIMARY)
    PROCESSING("PROCESSING", "处理中", "enum.payinOrderStatus.processing"),

    @Style(StyleType.WARNING)
    MANUAL_REVIEW("MANUAL_REVIEW", "人工", "enum.payinOrderStatus.manualReview"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "成功", "enum.payinOrderStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.payinOrderStatus.failed"),

    @Style(StyleType.DANGER)
    CLOSED("CLOSED", "关闭", "enum.payinOrderStatus.closed");

    private final String code;
    private final String label;
    private final String i18nKey;

    PayinOrderStatusEnum(String code, String label, String i18nKey) {
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
