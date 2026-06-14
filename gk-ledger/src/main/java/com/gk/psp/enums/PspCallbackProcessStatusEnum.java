package com.gk.psp.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.annotation.Style;
import com.gk.common.enums.StringCodeEnum;
import com.gk.common.enums.StyleType;

/**
 * PSP 回调处理状态。
 */
@EnumDict("pspCallbackProcessStatus")
public enum PspCallbackProcessStatusEnum implements StringCodeEnum {
    @Style(StyleType.INFO)
    INIT("INIT", "待处理", "enum.pspCallbackProcessStatus.init"),

    @Style(StyleType.SUCCESS)
    SUCCESS("SUCCESS", "成功", "enum.pspCallbackProcessStatus.success"),

    @Style(StyleType.DANGER)
    FAILED("FAILED", "失败", "enum.pspCallbackProcessStatus.failed"),

    @Style(StyleType.WARNING)
    IGNORED("IGNORED", "忽略", "enum.pspCallbackProcessStatus.ignored");

    private final String code;
    private final String label;
    private final String i18nKey;

    PspCallbackProcessStatusEnum(String code, String label, String i18nKey) {
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
