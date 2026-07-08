package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * 代付订单状态 */
@EnumDict("payoutOrderStatus")
public enum PayoutOrderStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    CREATED("CREATED", "已创建", "enum.payoutOrderStatus.created"),

    @Style(StyleType.WARNING)
    FROZEN("FROZEN", "已冻结", "enum.payoutOrderStatus.frozen"),

    @Style(StyleType.PRIMARY)
    PROCESSING("PROCESSING", "处理中", "enum.payoutOrderStatus.processing"),

    @Style(StyleType.WARNING)
    MANUAL_REVIEW("MANUAL_REVIEW", "人工", "enum.payoutOrderStatus.manualReview"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "成功", "enum.payoutOrderStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.payoutOrderStatus.failed"),

    @Style(StyleType.DANGER)
    CANCELLED("CANCELLED", "取消", "enum.payoutOrderStatus.cancelled");

    private final String code;
    private final String label;
    private final String i18nKey;

    PayoutOrderStatusEnum(String code, String label, String i18nKey) {
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
