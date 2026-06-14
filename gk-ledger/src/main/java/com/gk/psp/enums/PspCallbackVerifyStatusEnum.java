package com.gk.psp.enums;

import com.gk.common.annotation.EnumDict;
import com.gk.common.enums.StringCodeEnum;

/**
 * PSP 回调验签状态。
 */
@EnumDict("pspCallbackVerifyStatus")
public enum PspCallbackVerifyStatusEnum implements StringCodeEnum {
    INIT("INIT", "待验签", "enum.pspCallbackVerifyStatus.init"),
    SUCCESS("SUCCESS", "成功", "enum.pspCallbackVerifyStatus.success"),
    FAILED("FAILED", "失败", "enum.pspCallbackVerifyStatus.failed"),
    SKIPPED("SKIPPED", "跳过", "enum.pspCallbackVerifyStatus.skipped");

    private final String code;
    private final String label;
    private final String i18nKey;

    PspCallbackVerifyStatusEnum(String code, String label, String i18nKey) {
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
