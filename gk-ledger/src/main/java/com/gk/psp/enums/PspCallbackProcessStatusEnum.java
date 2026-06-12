package com.gk.psp.enums;

import com.gk.common.enums.StringCodeEnum;

/**
 * PSP 回调处理状态。
 */
public enum PspCallbackProcessStatusEnum implements StringCodeEnum {
    INIT("INIT", "待处理", "enum.pspCallbackProcessStatus.init"),
    SUCCESS("SUCCESS", "成功", "enum.pspCallbackProcessStatus.success"),
    FAILED("FAILED", "失败", "enum.pspCallbackProcessStatus.failed"),
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
