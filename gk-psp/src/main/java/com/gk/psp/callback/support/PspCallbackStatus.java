package com.gk.psp.callback.support;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * Canonical order statuses used by PSP callback parsing and processing.
 */
@EnumDict("pspCallbackStatus")
public enum PspCallbackStatus implements StringCodeEnum {
    @Style(StyleType.PRIMARY)
    PROCESSING("PROCESSING", "处理中", "enum.pspCallbackStatus.processing"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "成功", "enum.pspCallbackStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.pspCallbackStatus.failed"),

    @Style(StyleType.DANGER)
    CANCELLED("CANCELLED", "取消", "enum.pspCallbackStatus.cancelled"),

    @Style(StyleType.DANGER)
    CLOSED("CLOSED", "关闭", "enum.pspCallbackStatus.closed"),

    @Style(StyleType.WARNING)
    MANUAL_REVIEW("MANUAL_REVIEW", "人工", "enum.pspCallbackStatus.manualReview");

    private final String code;
    private final String label;
    private final String i18nKey;

    PspCallbackStatus(String code, String label, String i18nKey) {
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
