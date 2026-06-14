package com.gk.payment.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * 代付订单状态。
 */
@EnumDict("payoutOrderStatus")
public enum PayoutOrderStatusEnum implements StringCodeEnum {
    CREATED("CREATED", "已创建", "enum.payoutOrderStatus.created"),
    FROZEN("FROZEN", "已冻结", "enum.payoutOrderStatus.frozen"),
    PROCESSING("PROCESSING", "处理中", "enum.payoutOrderStatus.processing"),
    MANUAL_REVIEW("MANUAL_REVIEW", "待人工处理", "enum.payoutOrderStatus.manualReview"),
    SUCCESS("SUCCESS", "成功", "enum.payoutOrderStatus.success"),
    FAILED("FAILED", "失败", "enum.payoutOrderStatus.failed"),
    CANCELLED("CANCELLED", "已取消", "enum.payoutOrderStatus.cancelled");

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
